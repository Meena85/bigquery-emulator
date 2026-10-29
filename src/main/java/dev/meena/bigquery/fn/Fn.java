package dev.meena.bigquery.fn;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.BqTypes;
import dev.meena.bigquery.model.*;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;
import org.postgresql.util.PGInterval;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
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

    /**
     * Encodes a value read from Postgres the way the BigQuery REST API writes it: everything is a string
     * (INTEGER, FLOAT, NUMERIC, BOOLEAN, JSON, ...), TIMESTAMP is epoch seconds, BYTES is base64 and
     * repeated columns are lists of cells.
     */
    public static Object toBigQueryValue(Object value, BqTypes.BqType type) {
        return switch (value) {
            case null -> null;
            case byte[] bytes -> Base64.getEncoder().encodeToString(bytes);
            case java.sql.Array array -> throw new IllegalStateException("Arrays must be read with getArray(): " + array);
            case Object array when array.getClass().isArray() -> {
                var element = new BqTypes.BqType(type.type(), false);
                List<TableCell> cells = new ArrayList<>();
                for (int i = 0; i < Array.getLength(array); i++)
                    cells.add(new TableCell().v(toBigQueryValue(Array.get(array, i), element)));
                yield cells;
            }
            case OffsetDateTime dateTime -> epochSeconds(dateTime.toInstant());
            case java.sql.Timestamp timestamp -> "TIMESTAMP".equals(type.type())
                    ? epochSeconds(timestamp.toInstant())
                    : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timestamp.toLocalDateTime());
            case LocalDateTime dateTime -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
            case java.sql.Time time -> DateTimeFormatter.ISO_LOCAL_TIME.format(time.toLocalTime());
            case LocalTime time -> DateTimeFormatter.ISO_LOCAL_TIME.format(time);
            case BigDecimal decimal -> decimal.toPlainString();
            case PGInterval interval -> interval(interval);
            default -> String.valueOf(value);
        };
    }

    /** BigQuery's canonical interval: {@code [-]Y-M [-]D [-]H:M:S[.F]}. */
    private static String interval(PGInterval i) {
        boolean negativeMonths = i.getYears() < 0 || i.getMonths() < 0;
        boolean negativeTime = i.getHours() < 0 || i.getMinutes() < 0 || i.getSeconds() < 0;
        return (negativeMonths ? "-" : "") + Math.abs(i.getYears()) + "-" + Math.abs(i.getMonths())
                + " " + i.getDays()
                + " " + (negativeTime ? "-" : "") + Math.abs(i.getHours()) + ":" + Math.abs(i.getMinutes()) + ":"
                + BigDecimal.valueOf(Math.abs(i.getSeconds())).stripTrailingZeros().toPlainString();
    }

    /** Epoch seconds with microsecond precision, e.g. {@code 1577836800.123456}. */
    private static String epochSeconds(Instant instant) {
        BigDecimal seconds = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano() / 1_000, 6));
        return seconds.stripTrailingZeros().toPlainString();
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
        r.setRows(tableRows(result.columns(), result.rows()));
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
        r.setRows(tableRows(result.columns(), result.rows()));
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
        schema.setFields(columns.stream().map(column -> {
            BqTypes.BqType type = BqTypes.fromPg(column.databaseTypeName());
            return new TableFieldSchema()
                    .name(column.name())
                    .type(type.type())
                    .mode(type.repeated() ? "REPEATED" : "NULLABLE");
        }).toList());
        return schema;
    }

    public static List<TableRow> tableRows(List<Postgres.Column> columns, List<List<Object>> rows) {
        List<BqTypes.BqType> types = columns.stream().map(c -> BqTypes.fromPg(c.databaseTypeName())).toList();
        return rows.stream()
                .map(source -> {
                    List<TableCell> cells = new ArrayList<>(source.size());
                    for (int i = 0; i < source.size(); i++)
                        cells.add(new TableCell().v(toBigQueryValue(source.get(i), types.get(i))));
                    return new TableRow().f(cells);
                })
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
