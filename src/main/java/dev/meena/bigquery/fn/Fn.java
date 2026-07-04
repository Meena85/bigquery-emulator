package dev.meena.bigquery.fn;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.model.*;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.Response;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class Fn {

    @RegisterForReflection
    public record ErrorResponse(Error error) {
        public record Error(int code, String message, String status, List<Detail> errors) {
        }

        public record Detail(String reason, String message, String locationType) {
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
        return projectID.toLowerCase() + "$" + schema.toLowerCase();
    }

    public static Response bigQueryError(Response.Status status, String message) {
        ErrorResponse.Detail detail = new ErrorResponse.Detail(
                status == Response.Status.BAD_REQUEST ? "invalidQuery" : "backendError",
                message,
                "other"
        );
        ErrorResponse.Error error = new ErrorResponse.Error(
                status.getStatusCode(),
                message,
                status.name(),
                List.of(detail)
        );
        return Response.status(status).entity(new ErrorResponse(error)).build();
    }

    public static GetQueryResultsResponse queryResultsResponse(String projectId,
                                                               String jobId,
                                                               String location,
                                                               Postgres.QueryResult result) {
        var r = new GetQueryResultsResponse(null, null);
        r.setEtag("\"" + jobId + "\"");
        r.setJobComplete(true);
        r.setTotalRows(String.valueOf(result.rows().size()));
        r.setCacheHit(false);

        var jobRef = new JobReference();
        jobRef.setProjectId(projectId);
        jobRef.setJobId(jobId);
        jobRef.setLocation(location);
        r.setJobReference(jobRef);

        if (r.getRows() == null)
            r.setRows(new ArrayList<>());

        for (List<Object> sourceRow : result.rows()) {
            var row = new TableRow();
            if (row.getF() == null)
                row.setF(new ArrayList<>());
            for (Object rawValue : sourceRow) {
                var cell = new TableCell();
                cell.setV(Fn.toBigQueryValue(rawValue));
                row.getF().add(cell);
            }
            r.getRows().add(row);
        }

        var schema = new TableSchema();
        r.setSchema(schema);

        if (schema.getFields() == null)
            schema.setFields(new ArrayList<>());

        for (Postgres.Column column : result.columns()) {
            var field = new TableFieldSchema();
            field.setName(column.name());
            field.setType(Fn.toBigQueryType(column.jdbcType()));
            field.setMode("NULLABLE");
            field.setFields(null);
            schema.getFields().add(field);
        }

        return r;
    }

    public static String pgQuote(String identifier) {
        if (isBlank(identifier)) return "\"\"";
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
