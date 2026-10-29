package dev.meena.bigquery.db;

import dev.meena.bigquery.dialect.Bq2Psql;
import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class Postgres {

    private static final Logger log = Logger.getLogger("Postgres");

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    AgroalDataSource dataSource;

    @SuppressWarnings("SqlSourceToSinkFlow")
    public QueryResult query(String query) {
        log.log(Level.INFO, "[SQL] {0}", query);
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement()) {
            boolean hasResultSet = st.execute(query);
            if (!hasResultSet) {
                return new QueryResult(List.of(), List.of());
            }

            try (ResultSet rs = st.getResultSet()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int numColumns = metaData.getColumnCount();

                List<Column> columns = new ArrayList<>();
                for (int i = 1; i <= numColumns; i++) {
                    columns.add(new Column(
                            metaData.getColumnLabel(i),
                            metaData.getColumnType(i),
                            metaData.getColumnTypeName(i)
                    ));
                }

                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= numColumns; i++) {
                        row.add(readValue(rs, i, columns.get(i - 1).databaseTypeName()));
                    }
                    rows.add(row);
                }

                return new QueryResult(columns, rows);
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[QUERY] query=" + query, ex);
            throw new IllegalStateException("Unable to execute query, sql=" + query, ex);
        }
    }

    /** Reads a column with a Java type that keeps full precision (no JVM time zone, no truncated micros). */
    private static Object readValue(ResultSet rs, int index, String pgType) throws SQLException {
        String type = pgType == null ? "" : pgType.toLowerCase(Locale.ROOT);
        if (type.startsWith("_")) {
            Array array = rs.getArray(index);
            return array == null ? null : array.getArray();
        }

        return switch (type) {
            case "timestamp" -> rs.getObject(index, LocalDateTime.class);
            case "timestamptz" -> rs.getObject(index, OffsetDateTime.class);
            case "date" -> rs.getObject(index, LocalDate.class);
            case "time" -> rs.getObject(index, LocalTime.class);
            case "bytea" -> rs.getBytes(index);
            default -> rs.getObject(index);
        };
    }

    public long count(String query) {
        //noinspection SqlSourceToSinkFlow
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next())
                return rs.getLong(1);
            else
                throw new IllegalStateException("Unable to count rows, query=" + query);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[COUNT] table=" + query + ", error: " + ex.getMessage());
            throw new IllegalStateException("Unable to count rows, sql=" + query, ex);
        }
    }

    public boolean isEmpty(String tableName) {
        //noinspection SqlSourceToSinkFlow
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT NOT exists(SELECT 1 FROM " + tableName + ")")) {
            if (rs.next())
                return rs.getBoolean(1);
            else
                throw new IllegalStateException("Unable to count rows in table: " + tableName);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[COUNT] " + tableName + "error: " + ex.getMessage());
            throw new IllegalStateException("Unable to count rows in table: " + tableName, ex);
        }
    }

    public String resolveTableName(String schema, String table) {
        if (schema == null || table == null) return null;
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE LOWER(table_schema) = LOWER(?)
                  AND LOWER(table_name) = LOWER(?)
                LIMIT 1
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, table);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[RESOLVE_TABLE] " + schema + "." + table + " error: " + ex.getMessage());
            throw new IllegalStateException("Unable to resolve table: " + schema + "." + table, ex);
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public void execute(String sql) {
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement()) {
            st.execute(sql);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[EXECUTE] " + ex.getMessage());
            throw new IllegalStateException("Unable to execute SQL: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public void insertAll(Bq2Psql.InsertAll insertAll) {
        if (insertAll == null)
            throw new IllegalArgumentException("Insert request is required");

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(insertAll.preparedSql())) {
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            Map<String, ColumnDefinition> columnDefinitions = getColumnDefinitions(
                    con,
                    insertAll.schema(),
                    insertAll.table()
            );

            for (Object[] row : insertAll.rows()) {
                for (int i = 0; i < row.length; i++) {
                    String columnName = insertAll.columns().get(i);
                    ColumnDefinition columnDefinition = columnDefinitions.get(columnName.toLowerCase(Locale.ROOT));

                    if (columnDefinition == null)
                        throw new IllegalArgumentException("Column not found: " + insertAll.schema() + "." + insertAll.table() + "." + columnName);

                    setStatementValue(stmt, i + 1, row[i], columnDefinition);
                }

                int i = stmt.executeUpdate();
                log.log(Level.INFO, "[INSERT] " + i + " row(s) inserted into " + insertAll.schema() + "." + insertAll.table());
            }
            con.commit();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[INSERT] " + ex.getMessage());
            throw new IllegalStateException("Unable to insert rows: " + ex.getMessage(), ex);
        }
    }

    private Map<String, ColumnDefinition> getColumnDefinitions(Connection con, String schema, String table) throws SQLException {
        Map<String, ColumnDefinition> columns = new HashMap<>();

        DatabaseMetaData metaData = con.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, schema, table, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int jdbcType = rs.getInt("DATA_TYPE");
                String databaseTypeName = rs.getString("TYPE_NAME");

                columns.put(
                        columnName.toLowerCase(Locale.ROOT),
                        new ColumnDefinition(columnName, jdbcType, databaseTypeName)
                );
            }
        }

        return columns;
    }

    private void setStatementValue(PreparedStatement stmt, int parameterIndex, Object value, ColumnDefinition column) throws SQLException {
        if (value == null)
            stmt.setNull(parameterIndex, column.jdbcType());
        else
            stmt.setObject(parameterIndex, PgValues.toParameter(value, column.databaseTypeName()));
    }

    private record ColumnDefinition(String name, int jdbcType, String databaseTypeName) {
    }

    @RegisterForReflection
    public record QueryResult(List<Column> columns, List<List<Object>> rows) {
    }

    @RegisterForReflection
    public record Column(String name, int jdbcType, String databaseTypeName) {
    }
}