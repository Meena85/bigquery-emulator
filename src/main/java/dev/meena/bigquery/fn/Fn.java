package dev.meena.bigquery.fn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.model.*;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class Fn {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JobStatus DONE = new JobStatus(null, "DONE");

    public static JSONArray toSimpleJsonArray(Postgres.QueryResult result) {
        JSONArray json = new JSONArray();

        for (List<Object> row : result.rows()) {
            JSONObject object = new JSONObject();

            for (int i = 0; i < result.columns().size(); i++) {
                object.put(result.columns().get(i).name(), row.get(i));
            }

            json.add(object);
        }

        return json;
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

    public static String decodeRequestBody(String contentEncoding, byte[] requestBody) {
        if (requestBody == null || requestBody.length == 0) {
            return "";
        }

        boolean gzipEncoded = contentEncoding != null
                && contentEncoding.toLowerCase().contains("gzip");

        try {
            if (gzipEncoded || looksLikeGzip(requestBody)) {
                try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(requestBody))) {
                    return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            return new String(requestBody, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BadRequestException("Unable to decode request body", e);
        }
    }

    public static boolean looksLikeGzip(byte[] bytes) {
        return bytes.length >= 2
                && (bytes[0] & 0xff) == 0x1f
                && (bytes[1] & 0xff) == 0x8b;
    }

    /**
     * Combine projectId and schema into a single string.
     * The resulting string is in the format "projectId|schema".
     *
     * @param projectID The project ID.
     * @param schema    The schema name.
     * @return The combined string.
     */
    public static String toSchema(String projectID, String schema) {
        if (projectID == null || schema == null) return schema;
        return projectID.toLowerCase() + "|" + schema.toLowerCase();
    }

    public record ErrorResponse(Error error) {
        public record Error(int code, String message, String status, List<Detail> errors) {
        }

        public record Detail(String reason, String message, String locationType) {
        }
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

    public static GetQueryResultsResponse toBigQueryQueryResponse(String projectId,
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

    public static String extractQuery(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(requestBody);

            JsonNode directQuery = root.get("query");
            if (directQuery != null && directQuery.isTextual()) {
                return directQuery.asText();
            }

            JsonNode wrappedQuery = root.path("configuration").path("query").path("query");
            if (wrappedQuery.isTextual()) {
                return wrappedQuery.asText();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String jobKey(String projectId, String jobId) {
        return projectId + ":" + jobId;
    }

    public static String extractRequestedJobId(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(requestBody);
            JsonNode jobId = root.path("jobReference").path("jobId");
            return jobId.isTextual() ? jobId.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractRequestedLocation(String requestBody) {
        if (isBlank(requestBody))
            return null;

        try {
            JsonNode root = OBJECT_MAPPER.readTree(requestBody);
            JsonNode location = root.path("jobReference").path("location");
            return location.isTextual() ? location.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Job toJob(
            String projectId,
            String jobId,
            String location,
            String query,
            TableReference destinationTable,
            Postgres.QueryResult result,
            long startTime,
            long endTime,
            String errorMessage
    ) {
        JobReference jobRef = new JobReference();
        jobRef.setProjectId(projectId);
        jobRef.setJobId(jobId);
        jobRef.setLocation(location);

        JobConfigurationQuery jobConfig = new JobConfigurationQuery();
        jobConfig.setQuery(query);
        jobConfig.setUseLegacySql(false);
        jobConfig.setDestinationTable(destinationTable);

        JobConfiguration configuration = new JobConfiguration();
        configuration.setQuery(jobConfig);

        Job response = new Job("\"" + jobId + "\"",
                projectId + ":" + location + "." + jobId,
                "bigquery#job",
                null,
                "/bigquery/v2/projects/" + projectId + "/jobs/" + jobId,
                "db@bigquery.local");
        response.setStatus(errorMessage == null ? DONE : toErrorStatus("invalidQuery", "query", errorMessage));
        response.setStatistics(toJobStatistics(startTime, endTime, result));
        response.setConfiguration(configuration);
        response.setJobReference(jobRef);

        return response;
    }

    public static String quotePostgresIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static JSONObject toBigQueryTableDataList(Postgres.QueryResult result) {
        JSONObject response = new JSONObject();
        response.put("kind", "bigquery#tableDataList");
        response.put("totalRows", String.valueOf(result.rows().size()));

        JSONArray rows = new JSONArray();
        for (List<Object> sourceRow : result.rows()) {
            JSONObject row = new JSONObject();
            JSONArray values = new JSONArray();

            for (Object rawValue : sourceRow) {
                JSONObject value = new JSONObject();
                value.put("v", Fn.toBigQueryValue(rawValue));
                values.add(value);
            }

            row.put("f", values);
            rows.add(row);
        }

        response.put("rows", rows);
        return response;
    }

    public static TableReference extractDestinationTable(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return null;
        }
        TableReference tableReference = new TableReference();

        try {
            JsonNode root = OBJECT_MAPPER.readTree(requestBody);
            JsonNode destinationTable = root.path("configuration").path("query").path("destinationTable");

            if (destinationTable.isMissingNode() || destinationTable.isNull()) {
                System.out.println("DESTINATION TABLE: not in configuration > query > destinationTable");
                return null;
            }

            String projectId = textOrNull(destinationTable.path("projectId"));
            String datasetId = textOrNull(destinationTable.path("datasetId"));
            String tableId = textOrNull(destinationTable.path("tableId"));
            if (datasetId == null || datasetId.isBlank() || tableId == null || tableId.isBlank()) {
                System.out.println("DESTINATION TABLE: datasetId or tableId is null or blank");
                return null;
            }

            tableReference.setProjectId(projectId);
            tableReference.setDatasetId(datasetId);
            tableReference.setTableId(tableId);

            return tableReference;
        } catch (Exception e) {
            System.out.println("DESTINATION TABLE: exception occurred - " + e.getMessage());
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    public static JobStatistics toJobStatistics(long startTime, long endTime, Postgres.QueryResult result) {
        JobStatistics stats = new JobStatistics(
                1.0,
                String.valueOf(startTime),
                String.valueOf(endTime),
                String.valueOf(endTime - startTime),
                "1",
                "parentId",
                List.of(),
                List.of(),
                null,
                String.valueOf(startTime),
                "1024",
                "2000");

        if (result != null)
            stats.setQuery(toQueryStats(result));

        return stats;
    }

    private static @NonNull JobStatistics2 toQueryStats(Postgres.QueryResult result) {
        JobStatistics2 queryStats = new JobStatistics2();

        TableSchema schema = new TableSchema();
        List<TableFieldSchema> fields = new ArrayList<>();
        for (Postgres.Column column : result.columns()) {
            TableFieldSchema field = new TableFieldSchema();
            field.setName(column.name());
            field.setType(Fn.toBigQueryType(column.jdbcType()));
            field.setMode("NULLABLE");
            fields.add(field);
        }
        schema.setFields(fields);
        queryStats.setSchema(schema);
        return queryStats;
    }

    private static JobStatus toErrorStatus(String reason, String location, String message) {
        ErrorProto err = new ErrorProto();
        err.setReason(reason);
        err.setLocation(location);
        err.setMessage(message);

        List<ErrorProto> errors = new ArrayList<>();
        errors.add(err);

        return new JobStatus(errors, "DONE");
    }
}
