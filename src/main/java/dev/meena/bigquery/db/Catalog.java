package dev.meena.bigquery.db;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read access to the Postgres catalog (schemas, tables, columns), which is how the emulator
 * exposes BigQuery datasets, tables and table schemas. All lookups are parameterized.
 */
@ApplicationScoped
public class Catalog {

    public record TableInfo(String name, boolean view) {
    }

    public record ColumnInfo(String name, String pgType, boolean nullable) {
    }

    public record TablePage(List<TableInfo> tables, long total) {
    }

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    AgroalDataSource dataSource;

    /** Schemas whose name starts with {@code prefix} (e.g. {@code "proj$"}), sorted by name. */
    public List<String> schemasWithPrefix(String prefix) {
        return queryStrings("""
                SELECT schema_name FROM information_schema.schemata
                WHERE starts_with(schema_name, ?)
                ORDER BY schema_name
                """, prefix);
    }

    public boolean schemaExists(String schema) {
        return !queryStrings("SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?", schema).isEmpty();
    }

    public boolean schemaHasTables(String schema) {
        return !queryStrings("SELECT table_name FROM information_schema.tables WHERE table_schema = ? LIMIT 1", schema).isEmpty();
    }

    public TablePage listTables(String schema, long offset, long limit) {
        List<TableInfo> tables = new ArrayList<>();
        long total;
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement("""
                    SELECT table_name, table_type FROM information_schema.tables
                    WHERE table_schema = ? AND table_type IN ('BASE TABLE', 'VIEW')
                    ORDER BY table_name OFFSET ? LIMIT ?
                    """)) {
                stmt.setString(1, schema);
                stmt.setLong(2, offset);
                stmt.setLong(3, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next())
                        tables.add(new TableInfo(rs.getString(1), "VIEW".equals(rs.getString(2))));
                }
            }
            try (PreparedStatement stmt = con.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = ? AND table_type IN ('BASE TABLE', 'VIEW')
                    """)) {
                stmt.setString(1, schema);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to list tables of " + schema + ": " + e.getMessage(), e);
        }
        return new TablePage(tables, total);
    }

    /** Finds a table or view by case-insensitive name, as BigQuery clients may differ in casing. */
    public TableInfo findTable(String schema, String table) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement("""
                     SELECT table_name, table_type FROM information_schema.tables
                     WHERE LOWER(table_schema) = LOWER(?) AND LOWER(table_name) = LOWER(?)
                       AND table_type IN ('BASE TABLE', 'VIEW')
                     LIMIT 1
                     """)) {
            stmt.setString(1, schema);
            stmt.setString(2, table);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? new TableInfo(rs.getString(1), "VIEW".equals(rs.getString(2))) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to look up table " + schema + "." + table + ": " + e.getMessage(), e);
        }
    }

    public List<ColumnInfo> columns(String schema, String table) {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement("""
                     SELECT column_name, udt_name, is_nullable FROM information_schema.columns
                     WHERE table_schema = ? AND table_name = ?
                     ORDER BY ordinal_position
                     """)) {
            stmt.setString(1, schema);
            stmt.setString(2, table);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    columns.add(new ColumnInfo(rs.getString(1), rs.getString(2), !"NO".equalsIgnoreCase(rs.getString(3))));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read columns of " + schema + "." + table + ": " + e.getMessage(), e);
        }
        return columns;
    }

    private List<String> queryStrings(String sql, String parameter) {
        List<String> values = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, parameter);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    values.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Catalog query failed: " + e.getMessage(), e);
        }
        return values;
    }
}
