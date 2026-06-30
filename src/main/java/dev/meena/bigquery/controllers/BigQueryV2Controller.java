package dev.meena.bigquery.controllers;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.Bq2Psql;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.fn.JobStorage;
import dev.meena.bigquery.model.*;
import dev.meena.bigquery.types.BigQueryError;
import dev.meena.bigquery.types.JobKey;
import dev.meena.bigquery.types.UnimplementedResponse;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.calcite.sql.parser.SqlParseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Path("/bigquery/v2/")
public class BigQueryV2Controller {

    private static final Logger log = Logger.getLogger("BigQueryV2Controller");

    @Inject
    Postgres postgres;

    @Inject
    JobStorage jobStorage;

    @Inject
    Bq2Psql bq;

    @POST
    @Path("/projects/{projectId}/queries")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response queries(
            @PathParam("projectId") String projectId,
            QueryRequest request) {

        if (isBlank(request.getQuery()))
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'query' parameter is required");

        try {
            Postgres.QueryResult result = postgres.query(bq.translate(request.getQuery(), projectId));
            return Response.ok(Fn.toBigQueryQueryResponse(projectId, "local-query-job", "US", result)).build();
        } catch (SqlParseException e) {
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            return Fn.bigQueryError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @POST
    @Path("/projects/{projectId}/datasets/{datasetId}/tables/{tableId}/insertAll")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertAll(
            @PathParam("projectId") String projectId,
            @PathParam("datasetId") String datasetId,
            @PathParam("tableId") String tableId,
            TableDataInsertAllRequest requestBody) {
        if (requestBody == null || requestBody.getRows() == null)
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'rows' parameter is required");

        try {
            postgres.insertAll(bq.insertAll(requestBody, projectId, datasetId, tableId));
            return Response.ok(new TableDataInsertAllResponse()).build();
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Error inserting data into Postgres: " + e.getMessage(), e);

            ErrorProto err = new ErrorProto();
            err.setMessage(e.getMessage());
            err.setReason("invalid");
            err.setLocation("rows[" + 0 + "]");

            TableDataInsertAllResponseInsertErrorsInner reason = new TableDataInsertAllResponseInsertErrorsInner();
            reason.setErrors(List.of(err));

            TableDataInsertAllResponse response = new TableDataInsertAllResponse();
            response.addInsertErrorsItem(reason);
            return Response.ok(response).build();
        }
    }

    @GET
    @Path("/projects/{projectId}/datasets/{datasetId}/tables/{tableId}/data")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTableData(
            @PathParam("projectId") String projectId,
            @PathParam("datasetId") String datasetId,
            @PathParam("tableId") String tableId,
            @QueryParam("startIndex") Long startIndex,
            @QueryParam("maxResults") Long maxResults) {
        long offset = startIndex == null || startIndex < 0 ? 0 : startIndex;
        long limit = maxResults == null || maxResults <= 0 ? 10_000 : maxResults;

        String sql = "SELECT * FROM " + Fn.quotePostgresIdentifier(Fn.toSchema(projectId, datasetId))
                + "." + Fn.quotePostgresIdentifier(tableId)
                + " OFFSET " + offset
                + " LIMIT " + limit;

        try {
            Postgres.QueryResult result = postgres.query(sql);
            if (result == null) {
                result = new Postgres.QueryResult(List.of(), List.of());
            }

            return Response.ok(Fn.toBigQueryTableDataList(result)).build();
        } catch (RuntimeException e) {
            return Fn.bigQueryError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GET
    @Path("/projects/{projectId}/queries/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobResults(
            @PathParam("projectId") String projectId,
            @PathParam("jobId") String jobId) {
        String jobKey = JobKey.of(projectId, jobId);
        var result = jobStorage.jobResults(jobKey);

        if (result == null)
            return Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Job " + jobKey);

        return Response.ok(result).build();
    }

    @GET
    @Path("/projects/{projectId}/queries/{jobId}/getQueryResults")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQueryResults(
            @PathParam("projectId") String projectId,
            @PathParam("jobId") String jobId) {
        return getJobResults(projectId, jobId);
    }

    @POST
    @Path("/projects/{projectId}/jobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(
            @PathParam("projectId") String projectId,
            @HeaderParam(HttpHeaders.CONTENT_ENCODING) String contentEncoding,
            byte[] requestBody) {
        String body = Fn.decodeRequestBody(contentEncoding, requestBody);
        String query = Fn.extractQuery(body);

        if (query == null || query.isBlank())
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'configuration.query.query' parameter is required");

        String jobId = Fn.extractRequestedJobId(body);
        if (isBlank(jobId)) {
            jobId = "job_" + UUID.randomUUID().toString().replace("-", "");
        }

        String location = Fn.extractRequestedLocation(body);
        if (location == null || location.isBlank()) {
            location = "US";
        }

        long startTime = Instant.now().toEpochMilli();

        TableReference destinationTable = Fn.extractDestinationTable(body);
        if (destinationTable == null) {
            destinationTable = new TableReference();
            destinationTable.setProjectId(projectId);
            destinationTable.setDatasetId("virtual");
            destinationTable.setTableId(jobId);
        }
        try {
            Postgres.QueryResult result = postgres.query(bq.translate(query, projectId));
            if (result == null) {
                result = new Postgres.QueryResult(List.of(), List.of());
            }

            long endTime = Instant.now().toEpochMilli();
            Job job = Fn.toJob(projectId, jobId, location, query, destinationTable, result, startTime, endTime, null);
            jobStorage.put(Fn.jobKey(projectId, jobId), job, Fn.toBigQueryQueryResponse(projectId, jobId, location, result));

            return Response.ok(job).build();
        } catch (SqlParseException e) {
            long endTime = Instant.now().toEpochMilli();
            Job job = Fn.toJob(projectId, jobId, location, query, destinationTable, null, startTime, endTime, e.getMessage());
            jobStorage.put(Fn.jobKey(projectId, jobId), job, null);

            Response.Status status = Response.Status.BAD_REQUEST;
            return Response.status(status)
                    .entity(new BigQueryError(e.getMessage(), status))
                    .build();
        } catch (Exception e) {
            long endTime = Instant.now().toEpochMilli();
            Job job = Fn.toJob(projectId, jobId, location, query, destinationTable, null, startTime, endTime, e.getMessage());
            jobStorage.put(Fn.jobKey(projectId, jobId), job, null);

            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                    .entity(new BigQueryError(e.getMessage(), status))
                    .build();
        }
    }

    @GET
    @Path("/projects/{projectId}/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJob(
            @PathParam("projectId") String projectId,
            @PathParam("jobId") String jobId) {
        Job job = jobStorage.jobDefinition(Fn.jobKey(projectId, jobId));
        if (job == null)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new BigQueryError("Not found: Job " + projectId + ":" + jobId, Response.Status.NOT_FOUND))
                    .build();

        return Response.ok(job).build();
    }


    @GET
    @Path("/projects/{projectId}/datasets/{datasetId}/tables")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTables(
            @PathParam("projectId") String projectId,
            @PathParam("datasetId") String datasetId,
            @QueryParam("maxResults") Integer maxResults,
            @QueryParam("pageToken") String pageToken) {
        int offset = parsePageToken(pageToken);
        int limit = maxResults == null || maxResults <= 0 ? 1_000 : maxResults;
        int fetchLimit = limit + 1;

        String tableSql = """
                SELECT table_name, table_type
                FROM information_schema.tables
                WHERE table_schema = '%s'
                  AND table_type IN ('BASE TABLE', 'VIEW')
                ORDER BY table_name
                OFFSET %d
                LIMIT %d
                """.formatted(escapeSqlLiteral(datasetId), offset, fetchLimit);

        String countSql = """
                SELECT COUNT(*) AS total_items
                FROM information_schema.tables
                WHERE table_schema = '%s'
                  AND table_type IN ('BASE TABLE', 'VIEW')
                """.formatted(escapeSqlLiteral(datasetId));

        try {
            Postgres.QueryResult tableResult = postgres.query(tableSql);
            Postgres.QueryResult countResult = postgres.query(countSql);

            int totalItems = extractCount(countResult);
            List<TableListTablesInner> tables = new ArrayList<>();

            for (int i = 0; i < tableResult.rows().size() && i < limit; i++) {
                List<Object> row = tableResult.rows().get(i);

                String tableName = String.valueOf(row.get(0));
                String postgresTableType = String.valueOf(row.get(1));
                String bigQueryTableType = "VIEW".equalsIgnoreCase(postgresTableType) ? "VIEW" : "TABLE";

                TableReference tableReference = new TableReference();
                tableReference.setProjectId(projectId);
                tableReference.setDatasetId(datasetId);
                tableReference.setTableId(tableName);

                TableListTablesInner table = new TableListTablesInner();
                table.setKind("bigquery#table");
                table.setId(projectId + ":" + datasetId + "." + tableName);
                table.setFriendlyName(tableName);
                table.setTableReference(tableReference);
                table.setType(bigQueryTableType);

                tables.add(table);
            }

            TableList response = new TableList();
            response.setKind("bigquery#tableList");
            response.setEtag("\"" + projectId + ":" + datasetId + ":tables\"");
            response.setTables(tables);
            response.setTotalItems(totalItems);

            if (tableResult.rows().size() > limit) {
                response.setNextPageToken(String.valueOf(offset + limit));
            }

            return Response.ok(response).build();
        } catch (RuntimeException e) {
            return Fn.bigQueryError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GET
    @Path("/projects/{projectId}/datasets/{datasetId}/tables/{tableId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTable(
            @PathParam("projectId") String projectId,
            @PathParam("datasetId") String datasetId,
            @PathParam("tableId") String tableId,
            @QueryParam("selectedFields") String selectedFields,
            @QueryParam("view") String view) {
        String tableSql = """
                SELECT table_name, table_type
                FROM information_schema.tables
                WHERE table_schema = '%s'
                  AND table_name = '%s'
                  AND table_type IN ('BASE TABLE', 'VIEW')
                LIMIT 1
                """.formatted(escapeSqlLiteral(Fn.toSchema(projectId, datasetId)), escapeSqlLiteral(tableId));

        String columnsSql = """
                SELECT column_name, data_type, is_nullable, ordinal_position
                FROM information_schema.columns
                WHERE table_schema = '%s'
                  AND table_name = '%s'
                ORDER BY ordinal_position
                """.formatted(escapeSqlLiteral(Fn.toSchema(projectId, datasetId)), escapeSqlLiteral(tableId));

        String rowCountSql = """
                SELECT COUNT(*) AS row_count
                FROM %s.%s
                """.formatted(Fn.quotePostgresIdentifier(Fn.toSchema(projectId, datasetId)), Fn.quotePostgresIdentifier(tableId));

        try {
            Postgres.QueryResult tableResult = postgres.query(tableSql);
            if (tableResult == null || tableResult.rows().isEmpty()) {
                return Fn.bigQueryError(
                        Response.Status.NOT_FOUND,
                        "Not found: Table " + projectId + ":" + datasetId + "." + tableId
                );
            }

            List<Object> tableRow = tableResult.rows().getFirst();
            String postgresTableType = String.valueOf(tableRow.get(1));
            String bigQueryTableType = "VIEW".equalsIgnoreCase(postgresTableType) ? "VIEW" : "TABLE";

            Postgres.QueryResult columnsResult = postgres.query(columnsSql);
            Postgres.QueryResult rowCountResult = postgres.query(rowCountSql);

            TableReference tableReference = new TableReference();
            tableReference.setProjectId(projectId);
            tableReference.setDatasetId(datasetId);
            tableReference.setTableId(tableId);

            TableSchema schema = new TableSchema();
            List<TableFieldSchema> fields = new ArrayList<>();

            if (columnsResult != null) {
                for (List<Object> columnRow : columnsResult.rows()) {
                    String columnName = String.valueOf(columnRow.get(0));
                    String postgresDataType = String.valueOf(columnRow.get(1));
                    String isNullable = String.valueOf(columnRow.get(2));

                    if (!shouldIncludeSelectedField(selectedFields, columnName)) {
                        continue;
                    }

                    TableFieldSchema field = new TableFieldSchema();
                    field.setName(columnName);
                    field.setType(toBigQueryType(postgresDataType));
                    field.setMode("NO".equalsIgnoreCase(isNullable) ? "REQUIRED" : "NULLABLE");

                    fields.add(field);
                }
            }

            schema.setFields(fields);

            Table table = new Table(
                    "0",
                    "\"" + projectId + ":" + datasetId + "." + tableId + "\"",
                    projectId + ":" + datasetId + "." + tableId,
                    String.valueOf(System.currentTimeMillis()),
                    "US",
                    "1024",
                    "2048",
                    "1024",
                    null,
                    null,
                    null,
                    "2",
                    null,
                    String.valueOf(extractCount(rowCountResult)),
                    null,
                    null,
                    null,
                    null,
                    "/bigquery/v2/projects/" + projectId + "/datasets/" + datasetId + "/tables/" + tableId,
                    bigQueryTableType
            );
            table.setKind("bigquery#table");
            table.setFriendlyName(tableId);
            table.setTableReference(tableReference);
            table.setSchema(schema);

            return Response.ok(table).build();
        } catch (RuntimeException e) {
            return Fn.bigQueryError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Path("/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unimplementedBigQueryEndpoint(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            HttpServerRequest request,
            @HeaderParam(HttpHeaders.CONTENT_ENCODING) String contentEncoding,
            byte[] requestBody) {
        String body = Fn.decodeRequestBody(contentEncoding, requestBody);

        String requestMethod = request.method().name();
        String params = uriInfo.getQueryParameters()
                .entrySet().stream()
                .map((entry) -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));

        log.warning("Unimplemented BigQuery endpoint called:" +
                "\n [" + requestMethod + "] /bigquery/v2/" + path +
                "\n  parameters: " + params +
                "\n  body: " + body);

        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(new UnimplementedResponse(
                        "BigQuery endpoint is not implemented yet",
                        uriInfo.getRequestUri().toString(),
                        "/bigquery/v2/" + path,
                        uriInfo.getQueryParameters().toString(),
                        body))
                .build();
    }

    private static boolean shouldIncludeSelectedField(String selectedFields, String columnName) {
        if (isBlank(selectedFields)) {
            return true;
        }

        for (String selectedField : selectedFields.split(",")) {
            if (columnName.equalsIgnoreCase(selectedField.trim())) {
                return true;
            }
        }

        return false;
    }

    private static String toBigQueryType(String postgresDataType) {
        if (postgresDataType == null) {
            return "STRING";
        }

        return switch (postgresDataType.toLowerCase()) {
            case "bigint", "integer", "smallint", "serial", "bigserial", "smallserial" -> "INTEGER";
            case "numeric", "decimal", "real", "double precision" -> "FLOAT";
            case "boolean" -> "BOOLEAN";
            case "date" -> "DATE";
            case "time without time zone", "time with time zone" -> "TIME";
            case "timestamp without time zone", "timestamp with time zone" -> "TIMESTAMP";
            case "json", "jsonb" -> "JSON";
            case "bytea" -> "BYTES";
            default -> "STRING";
        };
    }

    private static int parsePageToken(String pageToken) {
        if (isBlank(pageToken)) {
            return 0;
        }

        try {
            return Math.max(0, Integer.parseInt(pageToken));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int extractCount(Postgres.QueryResult result) {
        if (result == null || result.rows().isEmpty() || result.rows().getFirst().isEmpty()) {
            return 0;
        }

        Object value = result.rows().getFirst().getFirst();
        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.parseInt(String.valueOf(value));
    }

    private static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
