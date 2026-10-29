package dev.meena.bigquery.fn;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.model.*;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class Fn {

    /** BigQuery's standard error envelope: {"error": {code, message, status, errors: [...]}}. */
    @RegisterForReflection(ignoreNested = false)
    public record ErrorResponse(Error error) {
        public record Error(int code, String message, String status, List<Detail> errors) {
        }

        public record Detail(String message, String domain, String reason, String locationType) {
        }
    }

    public static String toBigQueryType(int jdbcType) {
        return switch (jdbcType) {
            case Types.BIGINT, Types.INTEGER, Types.SMALLINT, Types.TINYINT -> "INTEGER";
            case Types.NUMERIC, Types.DECIMAL, Types.DOUBLE, Types.FLOAT, Types.REAL -> "FLOAT";
            case Types.BOOLEAN, Types.BIT -> "BOOLEAN";
            case Types.DATE -> "DATE";
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> "TIME";
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP";
            default -> "STRING";
        };
    }

    public static Object toBigQueryValue(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate.toString();
            case LocalDateTime localDateTime -> localDateTime.toString();
            case OffsetDateTime offsetDateTime -> offsetDateTime.toString();
            default -> String.valueOf(value);
        };
    }

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

    public static Response bigQueryError(Response.Status status, String message) {
        return bigQueryError(status, message, defaultReason(status));
    }

    public static Response bigQueryError(Response.Status status, String message, String reason) {
        var detail = new ErrorResponse.Detail(message, "global", reason, "other");
        var error = new ErrorResponse.Error(status.getStatusCode(), message, googleStatus(status), List.of(detail));
        return Response.status(status).entity(new ErrorResponse(error)).build();
    }

    /**
     * Map an exception to a BigQuery error response: SQL syntax problems are 400 {@code invalidQuery},
     * "Not found: ..." is 404, "Already Exists: ..." is 409 and anything else is a 500.
     */
    public static Response toError(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return switch (e) {
            case SqlParseException ignored -> bigQueryError(Response.Status.BAD_REQUEST, message, "invalidQuery");
            case IllegalArgumentException ignored -> bigQueryError(Response.Status.BAD_REQUEST, message, "invalid");
            default -> {
                if (message.startsWith("Not found")) yield bigQueryError(Response.Status.NOT_FOUND, message);
                if (message.startsWith("Already Exists")) yield bigQueryError(Response.Status.CONFLICT, message);
                yield bigQueryError(Response.Status.INTERNAL_SERVER_ERROR, message);
            }
        };
    }

    private static String defaultReason(Response.Status status) {
        return switch (status) {
            case BAD_REQUEST -> "invalid";
            case NOT_FOUND -> "notFound";
            case CONFLICT -> "duplicate";
            case NOT_IMPLEMENTED -> "notImplemented";
            default -> "backendError";
        };
    }

    private static String googleStatus(Response.Status status) {
        return switch (status) {
            case BAD_REQUEST -> "INVALID_ARGUMENT";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "ALREADY_EXISTS";
            case NOT_IMPLEMENTED -> "UNIMPLEMENTED";
            default -> "INTERNAL";
        };
    }

    public static GetQueryResultsResponse queryResultsResponse(String projectId, String jobId, String location,
                                                               Postgres.QueryResult result) {
        var r = new GetQueryResultsResponse(null, null);
        r.setEtag("\"" + jobId + "\"");
        r.setJobComplete(true);
        r.setCacheHit(false);
        r.setTotalRows(String.valueOf(result.rows().size()));
        r.setJobReference(jobReference(projectId, jobId, location));
        r.setSchema(tableSchema(result.columns()));
        r.setRows(tableRows(result.rows()));
        return r;
    }

    /** Response of the synchronous {@code POST /queries} call. */
    public static QueryResponse queryResponse(String projectId, String jobId, String location,
                                              Postgres.QueryResult result) {
        var r = new QueryResponse(null, null);
        r.setJobComplete(true);
        r.setCacheHit(false);
        r.setTotalRows(String.valueOf(result.rows().size()));
        r.setJobReference(jobReference(projectId, jobId, location));
        r.setSchema(tableSchema(result.columns()));
        r.setRows(tableRows(result.rows()));
        return r;
    }

    public static JobReference jobReference(String projectId, String jobId, String location) {
        var jobRef = new JobReference();
        jobRef.setProjectId(projectId);
        jobRef.setJobId(jobId);
        jobRef.setLocation(location);
        return jobRef;
    }

    public static TableSchema tableSchema(List<Postgres.Column> columns) {
        var schema = new TableSchema();
        schema.setFields(columns.stream().map(column -> new TableFieldSchema()
                .name(column.name())
                .type(toBigQueryType(column.jdbcType()))
                .mode("NULLABLE")).toList());
        return schema;
    }

    public static List<TableRow> tableRows(List<List<Object>> rows) {
        return rows.stream()
                .map(source -> new TableRow().f(source.stream()
                        .map(value -> new TableCell().v(toBigQueryValue(value)))
                        .toList()))
                .toList();
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
