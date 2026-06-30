package dev.meena.bigquery.db;

import dev.meena.bigquery.dialect.Bq2Psql;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();

            List<Column> columns = new ArrayList<>();
            for (int i = 1; i <= numColumns; i++) {
                columns.add(new Column(
                        rsmd.getColumnLabel(i),
                        rsmd.getColumnType(i),
                        rsmd.getColumnTypeName(i)
                ));
            }

            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= numColumns; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }

            return new QueryResult(columns, rows);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[QUERY] " + ex.getMessage());
            return new QueryResult(Collections.emptyList(), Collections.emptyList());
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public void insertAll(Bq2Psql.InsertAll insertAll) {
        if (insertAll == null) {
            throw new IllegalArgumentException("Insert request is required");
        }

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

                    if (columnDefinition == null) {
                        throw new IllegalArgumentException("Column not found: " + insertAll.schema() + "." + insertAll.table() + "." + columnName);
                    }

                    setStatementValue(stmt, i + 1, row[i], columnDefinition);
                }

                int i = stmt.executeUpdate();
            }
            con.commit();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[INSERT] " + ex.getMessage());
            throw new IllegalStateException("Unable to insert rows: " + ex.getMessage(), ex);
        }
    }

    public void logTablesAndColumns() {
        String sql = """
                SELECT
                    table_schema,
                    table_name,
                    column_name,
                    data_type,
                    is_nullable,
                    ordinal_position
                FROM information_schema.columns
                WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
                  AND table_schema NOT LIKE 'pg_toast%'
                ORDER BY table_schema, table_name, ordinal_position
                """;

        System.out.println("[DB SCHEMA] Tables and columns:");
        log.info("[DB SCHEMA] Tables and columns:");
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            String currentTable = null;
            boolean foundAnyColumn = false;

            int i = 0;
            while (rs.next()) {
                foundAnyColumn = true;

                String schema = rs.getString("table_schema");
                String table = rs.getString("table_name");
                String tableKey = schema + "." + table;

                if (!tableKey.equals(currentTable)) {
                    currentTable = tableKey;
                    log.info("[DB SCHEMA] " + tableKey);
                }

                log.info("[DB SCHEMA]   - "
                        + rs.getString("column_name")
                        + " "
                        + rs.getString("data_type")
                        + " nullable=" + rs.getString("is_nullable")
                        + " ordinal=" + rs.getInt("ordinal_position"));
                if (i++ == 6) break;
            }

            if (!foundAnyColumn) {
                log.warning("[DB SCHEMA] No user tables/columns found");
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "[DB SCHEMA] Unable to log tables and columns: " + ex.getMessage(), ex);
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
        if (value == null) {
            stmt.setNull(parameterIndex, column.jdbcType());
            return;
        }

        String databaseTypeName = column.databaseTypeName() == null
                ? ""
                : column.databaseTypeName().toLowerCase(Locale.ROOT);

        if ("json".equals(databaseTypeName) || "jsonb".equals(databaseTypeName)) {
            PGobject json = new PGobject();
            json.setType(databaseTypeName);
            json.setValue(String.valueOf(value));
            stmt.setObject(parameterIndex, json);
            return;
        }

        switch (column.jdbcType()) {
            case Types.DATE -> {
                if (value instanceof java.sql.Date date) {
                    stmt.setDate(parameterIndex, date);
                } else if (value instanceof LocalDate localDate) {
                    stmt.setDate(parameterIndex, Date.valueOf(localDate));
                } else {
                    stmt.setDate(parameterIndex, Date.valueOf(String.valueOf(value)));
                }
            }
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> {
                switch (value) {
                    case Timestamp timestamp -> stmt.setTimestamp(parameterIndex, timestamp);
                    case LocalDateTime localDateTime ->
                            stmt.setTimestamp(parameterIndex, Timestamp.valueOf(localDateTime));
                    case OffsetDateTime offsetDateTime -> stmt.setObject(parameterIndex, offsetDateTime);
                    default -> stmt.setTimestamp(parameterIndex, Timestamp.valueOf(String.valueOf(value)));
                }
            }
            case Types.BOOLEAN, Types.BIT -> {
                if (value instanceof Boolean bool) {
                    stmt.setBoolean(parameterIndex, bool);
                } else {
                    stmt.setBoolean(parameterIndex, Boolean.parseBoolean(String.valueOf(value)));
                }
            }
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> {
                if (value instanceof Number number) {
                    stmt.setInt(parameterIndex, number.intValue());
                } else {
                    stmt.setInt(parameterIndex, Integer.parseInt(String.valueOf(value)));
                }
            }
            case Types.BIGINT -> {
                if (value instanceof Number number) {
                    stmt.setLong(parameterIndex, number.longValue());
                } else {
                    stmt.setLong(parameterIndex, Long.parseLong(String.valueOf(value)));
                }
            }
            case Types.FLOAT, Types.REAL -> {
                if (value instanceof Number number) {
                    stmt.setFloat(parameterIndex, number.floatValue());
                } else {
                    stmt.setFloat(parameterIndex, Float.parseFloat(String.valueOf(value)));
                }
            }
            case Types.DOUBLE -> {
                if (value instanceof Number number) {
                    stmt.setDouble(parameterIndex, number.doubleValue());
                } else {
                    stmt.setDouble(parameterIndex, Double.parseDouble(String.valueOf(value)));
                }
            }
            case Types.NUMERIC, Types.DECIMAL -> {
                if (value instanceof BigDecimal bigDecimal) {
                    stmt.setBigDecimal(parameterIndex, bigDecimal);
                } else if (value instanceof Number number) {
                    stmt.setBigDecimal(parameterIndex, BigDecimal.valueOf(number.doubleValue()));
                } else {
                    stmt.setBigDecimal(parameterIndex, new BigDecimal(String.valueOf(value)));
                }
            }
            default -> stmt.setObject(parameterIndex, value, column.jdbcType());
        }
    }

    private record ColumnDefinition(String name, int jdbcType, String databaseTypeName) {
    }

    public record QueryResult(List<Column> columns, List<List<Object>> rows) {
    }

    public record Column(String name, int jdbcType, String databaseTypeName) {
    }
}