package dev.meena.db;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class Postgres {

    private static final Logger log = Logger.getLogger(Postgres.class.getName());

    @ConfigProperty(name = "db.url")
    String url;

    @ConfigProperty(name = "db.user")
    String user;

    @ConfigProperty(name = "db.password")
    String password;

    public QueryResult query(String query) {
        try (Connection con = DriverManager.getConnection(url, user, password);
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
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public record QueryResult(List<Column> columns, List<List<Object>> rows) {
    }

    public record Column(String name, int jdbcType, String databaseTypeName) {
    }
}