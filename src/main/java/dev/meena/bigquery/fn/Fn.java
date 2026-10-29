package dev.meena.bigquery.fn;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Identifier and SQL text helpers.
 */
public class Fn {

    /**
     * Combine projectId and schema into a single string.
     * The resulting string is in the format "projectId$schema".
     *
     * @param projectID The project ID.
     * @param schema    The schema name.
     * @return The combined string.
     */
    public static String toSchema(String projectID, String schema) {
        if (projectID == null || schema == null) return schema;
        if ("information_schema".equalsIgnoreCase(schema)) return "information_schema";
        return projectID.toLowerCase() + "$" + schema.toLowerCase();
    }

    public static String pgQuote(String identifier) {
        if (isBlank(identifier)) return "\"\"";
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static String removeSemiColon(String sql) {
        if (sql != null && sql.endsWith(";"))
            return sql.substring(0, sql.length() - 1);

        return sql;
    }
}
