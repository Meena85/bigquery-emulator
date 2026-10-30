package dev.meena.bigquery.init;

import dev.meena.bigquery.db.PgValues;
import dev.meena.bigquery.dialect.Bq2Psql;
import dev.meena.bigquery.fn.Fn;
import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Loads the files of an init directory into a fresh database, once.
 * <p>
 * Files are processed in name order:
 * <ul>
 *   <li>{@code *.sql}: BigQuery SQL scripts ({@code CREATE TABLE}, {@code INSERT}, ...), translated like any query</li>
 *   <li>{@code *.csv}: rows for an existing table, named {@code dataset.table.csv} or {@code project.dataset.table.csv};
 *       the first row holds the column names</li>
 * </ul>
 * Everything runs in one transaction together with a marker row in the database, so a failed load leaves
 * nothing behind and is retried at the next start, while a database that has been loaded is never loaded again.
 * Recreate the database to load again.
 */
@ApplicationScoped
public class InitLoader {

    private static final Logger log = Logger.getLogger("InitLoader");
    private static final String MARKER = "initial-load";
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    private static final long LOCK_ID = 7_251_913_001L;

    @ConfigProperty(name = "emulator.init.dir")
    Optional<String> initDir;

    /** Project that unqualified (and csv) table names belong to. */
    @ConfigProperty(name = "emulator.init.project", defaultValue = "local")
    String defaultProject;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    AgroalDataSource dataSource;

    @Inject
    Bq2Psql bq;

    void onStart(@Observes StartupEvent event) {
        initDir.filter(dir -> !dir.isBlank()).ifPresent(dir -> load(Path.of(dir)));
    }

    /** @return true when the directory was loaded now, false when this database was already loaded or there is nothing to load */
    public boolean load(Path dir) {
        if (!Files.isDirectory(dir)) {
            log.warning("Init directory does not exist, skipping: " + dir);
            return false;
        }

        List<Path> files = listFiles(dir);
        if (files.isEmpty()) {
            log.info("Init directory has no .sql or .csv files: " + dir);
            return false;
        }

        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (Statement st = con.createStatement()) {
                    st.execute("CREATE SCHEMA IF NOT EXISTS \"bq$jobs\"");
                    st.execute("CREATE TABLE IF NOT EXISTS \"bq$jobs\".init_state "
                            + "(name TEXT PRIMARY KEY, loaded_at TIMESTAMPTZ NOT NULL DEFAULT now(), files TEXT)");
                }
                con.commit();

                // Serialize concurrent starts; the lock is released at commit/rollback
                try (Statement st = con.createStatement()) {
                    st.execute("SELECT pg_advisory_xact_lock(" + LOCK_ID + ")");
                }
                if (alreadyLoaded(con)) {
                    con.rollback();
                    log.info("Database was already initialized, not loading " + dir);
                    return false;
                }

                for (Path file : files) {
                    log.info("Loading " + file.getFileName());
                    if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sql"))
                        runSql(con, file);
                    else
                        loadCsv(con, file);
                }

                try (PreparedStatement stmt = con.prepareStatement("INSERT INTO \"bq$jobs\".init_state (name, files) VALUES (?, ?)")) {
                    stmt.setString(1, MARKER);
                    stmt.setString(2, files.stream().map(f -> f.getFileName().toString()).toList().toString());
                    stmt.executeUpdate();
                }
                con.commit();
                log.info("Loaded " + files.size() + " init file(s) from " + dir);
                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Loading init files failed, nothing was changed: " + e.getMessage());
            throw new IllegalStateException("Loading init files from " + dir + " failed: " + e.getMessage(), e);
        }
    }

    private static List<Path> listFiles(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(f -> {
                        String name = f.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".sql") || name.endsWith(".csv")) return true;
                        if (!name.startsWith(".") && !name.endsWith(".md"))
                            log.warning("Ignoring init file (not .sql or .csv): " + f.getFileName());
                        return false;
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list " + dir + ": " + e.getMessage(), e);
        }
    }

    private static boolean alreadyLoaded(Connection con) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement("SELECT 1 FROM \"bq$jobs\".init_state WHERE name = ?")) {
            stmt.setString(1, MARKER);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void runSql(Connection con, Path file) throws Exception {
        String script = Files.readString(file, StandardCharsets.UTF_8);
        for (String statement : SqlScript.split(script)) {
            String sql = bq.translate(statement, defaultProject);
            log.fine(() -> "[INIT] " + sql);
            try (Statement st = con.createStatement()) {
                st.execute(sql);
            } catch (SQLException e) {
                throw new IllegalStateException(file.getFileName() + ": " + e.getMessage() + "\n  statement: " + statement, e);
            }
        }
    }

    private void loadCsv(Connection con, Path file) throws Exception {
        String[] name = file.getFileName().toString().replaceFirst("(?i)\\.csv$", "").split("\\.");
        if (name.length < 2 || name.length > 3)
            throw new IllegalArgumentException(file.getFileName() + ": name the file dataset.table.csv or project.dataset.table.csv");
        String project = name.length == 3 ? name[0] : defaultProject;
        String schema = Fn.toSchema(project, name[name.length - 2]);
        String requested = name[name.length - 1];

        String table = resolveTable(con, schema, requested);
        if (table == null)
            throw new IllegalStateException(file.getFileName() + ": table " + schema + "." + requested + " does not exist, create it in an earlier .sql file");

        List<List<String>> records = Csv.parse(Files.readString(file, StandardCharsets.UTF_8));
        if (records.isEmpty())
            return;

        List<String> header = records.getFirst().stream().map(h -> h == null ? "" : h.trim().toLowerCase(Locale.ROOT)).toList();
        String qualified = Fn.pgQuote(schema) + "." + Fn.pgQuote(table);
        List<String> types = columnTypes(con, qualified, header, file);

        String sql = "INSERT INTO " + qualified + " (" + String.join(", ", header.stream().map(Fn::pgQuote).toList()) + ") VALUES ("
                + String.join(", ", java.util.Collections.nCopies(header.size(), "?")) + ")";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            int line = 1;
            for (List<String> record : records.subList(1, records.size())) {
                line++;
                if (record.size() != header.size())
                    throw new IllegalArgumentException(file.getFileName() + " line " + line + ": expected " + header.size() + " fields but found " + record.size());
                for (int i = 0; i < header.size(); i++) {
                    String value = record.get(i);
                    if (value == null)
                        stmt.setObject(i + 1, null);
                    else
                        stmt.setObject(i + 1, toParameter(value, types.get(i), file, line, header.get(i)));
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        log.info("Loaded " + (records.size() - 1) + " row(s) into " + schema + "." + table);
    }

    private static Object toParameter(String value, String type, Path file, int line, String column) {
        try {
            // Repeated columns are written as a JSON array, e.g. [1,2,3] or ["a","b"]
            if (type.startsWith("_") && value.stripLeading().startsWith("["))
                return PgValues.toParameter(JSON.readValue(value, java.util.List.class), type);
            return PgValues.toParameter(value, type);
        } catch (Exception e) {
            throw new IllegalArgumentException(file.getFileName() + " line " + line + ", column " + column + ": " + e.getMessage(), e);
        }
    }

    private static String resolveTable(Connection con, String schema, String table) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND LOWER(table_name) = LOWER(?)")) {
            stmt.setString(1, schema);
            stmt.setString(2, table);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Postgres type names of the header's columns, read from the table so values are converted for the right type. */
    private static List<String> columnTypes(Connection con, String qualifiedTable, List<String> header, Path file) throws SQLException {
        List<String> types = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + qualifiedTable + " WHERE false")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (String column : header) {
                String type = null;
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    if (meta.getColumnLabel(i).equalsIgnoreCase(column)) type = meta.getColumnTypeName(i);
                }
                if (type == null)
                    throw new IllegalArgumentException(file.getFileName() + ": table has no column '" + column + "'");
                types.add(type);
            }
        }
        return types;
    }
}
