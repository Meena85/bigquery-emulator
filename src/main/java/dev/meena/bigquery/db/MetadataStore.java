package dev.meena.bigquery.db;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static dev.meena.bigquery.fn.Fn.pgQuote;

/** Reads and writes {@link Metadata} as Postgres object comments. */
@ApplicationScoped
public class MetadataStore {

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    AgroalDataSource dataSource;

    @Inject
    Postgres postgres;

    public Metadata dataset(String schema) {
        return read("SELECT obj_description(oid, 'pg_namespace') FROM pg_namespace WHERE nspname = ?", schema);
    }

    public Metadata table(String schema, String table) {
        return read("""
                SELECT obj_description(c.oid, 'pg_class') FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ? AND c.relname = ?
                """, schema, table);
    }

    public void saveDataset(String schema, Metadata metadata) {
        postgres.execute("COMMENT ON SCHEMA " + pgQuote(schema) + " IS " + literal(metadata.toComment()));
    }

    public void saveTable(String schema, String table, Metadata metadata) {
        postgres.execute("COMMENT ON TABLE " + pgQuote(schema) + "." + pgQuote(table) + " IS " + literal(metadata.toComment()));
    }

    private Metadata read(String sql, String... parameters) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++)
                stmt.setString(i + 1, parameters[i]);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Metadata.fromComment(rs.getString(1)) : Metadata.EMPTY;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read metadata: " + e.getMessage(), e);
        }
    }

    private static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
