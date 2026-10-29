package dev.meena.bigquery.dialect;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversion rules between BigQuery column types and Postgres column types.
 *
 * <pre>
 * BigQuery                     Postgres
 * INT64 (INT, SMALLINT, ...)   BIGINT
 * FLOAT64                      DOUBLE PRECISION
 * NUMERIC[(p,s)]               NUMERIC(p,s)  (default NUMERIC(38,9))
 * BIGNUMERIC[(p,s)]            NUMERIC[(p,s)]
 * BOOL                         BOOLEAN
 * STRING[(n)]                  TEXT
 * BYTES[(n)]                   BYTEA
 * DATE / TIME                  DATE / TIME
 * DATETIME                     TIMESTAMP
 * TIMESTAMP                    TIMESTAMPTZ
 * GEOGRAPHY                    TEXT (WKT; no PostGIS required)
 * JSON, STRUCT&lt;..&gt;            JSONB
 * INTERVAL                     INTERVAL
 * RANGE&lt;DATE|DATETIME|TIMESTAMP&gt;  DATERANGE | TSRANGE | TSTZRANGE
 * ARRAY&lt;T&gt;                    T[]
 * </pre>
 */
public final class BqTypes {

    /** A BigQuery type as reported to clients: base type plus whether the column is REPEATED. */
    public record BqType(String type, boolean repeated) {
    }

    private static final Pattern PARAMETERS = Pattern.compile("^([A-Za-z0-9_]+)\\s*\\(\\s*(\\d+)\\s*(?:,\\s*(\\d+)\\s*)?\\)$");
    private static final Pattern ARRAY = Pattern.compile("(?is)^ARRAY\\s*<(.*)>$");
    private static final Pattern RANGE = Pattern.compile("(?is)^RANGE\\s*<(.*)>$");
    private static final Pattern STRUCT = Pattern.compile("(?is)^(STRUCT|RECORD)\\b.*$");

    private BqTypes() {
    }

    /** Converts a BigQuery type (as written in DDL or in a REST schema) into a Postgres type. */
    public static String toPg(String bqType) {
        if (bqType == null || bqType.isBlank())
            return "TEXT";

        String type = bqType.trim();

        Matcher array = ARRAY.matcher(type);
        if (array.matches())
            return toPg(array.group(1)) + "[]";

        Matcher range = RANGE.matcher(type);
        if (range.matches()) {
            return switch (range.group(1).trim().toUpperCase(Locale.ROOT)) {
                case "DATE" -> "DATERANGE";
                case "DATETIME" -> "TSRANGE";
                default -> "TSTZRANGE";
            };
        }

        if (STRUCT.matcher(type).matches())
            return "JSONB";

        String name = type.toUpperCase(Locale.ROOT);
        String precision = "";
        Matcher parameters = PARAMETERS.matcher(name);
        if (parameters.matches()) {
            name = parameters.group(1);
            precision = parameters.group(3) == null
                    ? "(" + parameters.group(2) + ")"
                    : "(" + parameters.group(2) + "," + parameters.group(3) + ")";
        }

        return switch (name) {
            case "INT64", "INT", "SMALLINT", "INTEGER", "BIGINT", "TINYINT", "BYTEINT" -> "BIGINT";
            case "FLOAT64", "FLOAT" -> "DOUBLE PRECISION";
            case "NUMERIC", "DECIMAL" -> "NUMERIC" + (precision.isEmpty() ? "(38,9)" : precision);
            case "BIGNUMERIC", "BIGDECIMAL" -> "NUMERIC" + precision;
            case "BOOL", "BOOLEAN" -> "BOOLEAN";
            case "BYTES" -> "BYTEA";
            case "DATE" -> "DATE";
            case "TIME" -> "TIME";
            case "DATETIME" -> "TIMESTAMP";
            case "TIMESTAMP" -> "TIMESTAMPTZ";
            case "JSON" -> "JSONB";
            case "INTERVAL" -> "INTERVAL";
            default -> "TEXT"; // STRING, GEOGRAPHY and anything unknown
        };
    }

    /**
     * Converts a Postgres type name (as {@code information_schema.columns.udt_name} or JDBC's
     * {@code getColumnTypeName} report it, e.g. {@code int8}, {@code _text}) into the BigQuery type
     * reported to clients.
     */
    public static BqType fromPg(String pgType) {
        if (pgType == null)
            return new BqType("STRING", false);

        String name = pgType.toLowerCase(Locale.ROOT);
        if (name.startsWith("_") || name.endsWith("[]"))
            return new BqType(fromPg(name.startsWith("_") ? name.substring(1) : name.substring(0, name.length() - 2)).type(), true);

        return new BqType(switch (name) {
            case "int2", "int4", "int8", "smallint", "integer", "bigint", "serial", "bigserial", "smallserial", "oid" -> "INTEGER";
            case "float4", "float8", "real", "double precision" -> "FLOAT";
            case "numeric", "decimal" -> "NUMERIC";
            case "bool", "boolean" -> "BOOLEAN";
            case "date" -> "DATE";
            case "time", "timetz", "time without time zone", "time with time zone" -> "TIME";
            case "timestamp", "timestamp without time zone" -> "DATETIME";
            case "timestamptz", "timestamp with time zone" -> "TIMESTAMP";
            case "json", "jsonb" -> "JSON";
            case "bytea" -> "BYTES";
            case "interval" -> "INTERVAL";
            default -> "STRING";
        }, false);
    }
}
