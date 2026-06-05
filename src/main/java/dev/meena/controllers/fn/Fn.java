package dev.meena.controllers.fn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.meena.db.Postgres;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class Fn {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    public static Response bigQueryError(Response.Status status, String message) {
        JSONObject error = new JSONObject();
        error.put("code", status.getStatusCode());
        error.put("message", message);
        error.put("status", status.name());

        JSONArray errors = new JSONArray();

        JSONObject detail = new JSONObject();
        detail.put("message", message);
        detail.put("reason", status == Response.Status.BAD_REQUEST ? "invalidQuery" : "backendError");
        detail.put("locationType", "other");
        errors.add(detail);

        error.put("errors", errors);

        JSONObject response = new JSONObject();
        response.put("error", error);

        return Response.status(status).entity(response).build();
    }

    public static JSONObject toBigQueryQueryResponse(
            String projectId,
            String jobId,
            String location,
            Postgres.QueryResult result
    ) {
        JSONObject response = new JSONObject();

        response.put("kind", "bigquery#getQueryResultsResponse");
        response.put("etag", "\"" + jobId + "\"");
        response.put("jobComplete", true);
        response.put("totalRows", String.valueOf(result.rows().size()));
        response.put("cacheHit", false);

        JSONObject jobReference = new JSONObject();
        jobReference.put("projectId", projectId);
        jobReference.put("jobId", jobId);
        jobReference.put("location", location);
        response.put("jobReference", jobReference);

        JSONObject schema = new JSONObject();
        JSONArray fields = new JSONArray();
        for (Postgres.Column column : result.columns()) {
            JSONObject field = new JSONObject();
            field.put("name", column.name());
            field.put("type", Fn.toBigQueryType(column.jdbcType()));
            field.put("mode", "NULLABLE");
            fields.add(field);
        }
        schema.put("fields", fields);
        response.put("schema", schema);

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
        if (requestBody == null || requestBody.isBlank()) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(requestBody);
            JsonNode location = root.path("jobReference").path("location");
            return location.isTextual() ? location.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject toBigQueryJob(
            String projectId,
            String jobId,
            String location,
            String query,
            Postgres.QueryResult result,
            long startTime,
            long endTime,
            String errorMessage
    ) {
        JSONObject job = new JSONObject();
        job.put("kind", "bigquery#job");
        job.put("etag", "\"" + jobId + "\"");
        job.put("id", projectId + ":" + location + "." + jobId);
        job.put("selfLink", "/bigquery/v2/projects/" + projectId + "/jobs/" + jobId);

        JSONObject jobReference = new JSONObject();
        jobReference.put("projectId", projectId);
        jobReference.put("jobId", jobId);
        jobReference.put("location", location);
        job.put("jobReference", jobReference);

        JSONObject destinationTable = new JSONObject();
        destinationTable.put("projectId", projectId);
        destinationTable.put("datasetId", "public");
        destinationTable.put("tableId", "integration_test");

        JSONObject configuration = new JSONObject();
        JSONObject queryConfiguration = new JSONObject();
        queryConfiguration.put("query", query);
        queryConfiguration.put("useLegacySql", false);
        queryConfiguration.put("destinationTable", destinationTable);
        configuration.put("query", queryConfiguration);
        job.put("configuration", configuration);

        JSONObject statistics = new JSONObject();
        statistics.put("creationTime", String.valueOf(startTime));
        statistics.put("startTime", String.valueOf(startTime));
        statistics.put("endTime", String.valueOf(endTime));

        JSONObject queryStatistics = new JSONObject();
        queryStatistics.put("totalBytesProcessed", "100");
        queryStatistics.put("totalBytesBilled", "100");
        queryStatistics.put("cacheHit", false);

        if (result != null) {
            queryStatistics.put("totalRows", String.valueOf(result.rows().size()));

            JSONObject schema = new JSONObject();
            JSONArray fields = new JSONArray();
            for (Postgres.Column column : result.columns()) {
                JSONObject field = new JSONObject();
                field.put("name", column.name());
                field.put("type", Fn.toBigQueryType(column.jdbcType()));
                field.put("mode", "NULLABLE");
                fields.add(field);
            }
            schema.put("fields", fields);
            queryStatistics.put("schema", schema);
        }

        statistics.put("query", queryStatistics);
        job.put("statistics", statistics);

        JSONObject status = new JSONObject();
        status.put("state", "DONE");

        if (errorMessage != null) {
            JSONObject errorResult = new JSONObject();
            errorResult.put("reason", "invalidQuery");
            errorResult.put("location", "query");
            errorResult.put("message", errorMessage);

            JSONArray errors = new JSONArray();
            errors.add(errorResult);

            status.put("errorResult", errorResult);
            status.put("errors", errors);
        }

        job.put("status", status);

        return job;
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
}
