package dev.meena.bigquery.controllers;

import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.fn.JobStorage;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.Bq2Psql;
import dev.meena.bigquery.model.*;
import dev.meena.bigquery.types.BigQueryError;
import dev.meena.bigquery.types.JobKey;
import dev.meena.bigquery.types.UnimplementedResponse;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

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

        log.info("<><><> Received query: " + request.getQuery());
        try {
            Postgres.QueryResult result = postgres.query(bq.translate(request.getQuery()));
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
            postgres.insertAll(bq.insertAll(requestBody, datasetId, tableId));
            return Response.ok(new TableDataInsertAllResponse()).build();
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Error inserting data into Postgres: " + e.getMessage(), e);

            ErrorProto err = new ErrorProto();
            err.setMessage(e.getMessage());
            err.setReason("invalid");
            err.setLocation( "rows[" + 0 + "]");

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

        String sql = "SELECT * FROM " + Fn.quotePostgresIdentifier(datasetId)
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

        try {
            Postgres.QueryResult result = postgres.query(bq.translate(query));
            if (result == null) {
                result = new Postgres.QueryResult(List.of(), List.of());
            }

            long endTime = Instant.now().toEpochMilli();
            JSONObject job = Fn.toBigQueryJob(projectId, jobId, location, query, result, startTime, endTime, null);
            jobStorage.put(Fn.jobKey(projectId, jobId), job, Fn.toBigQueryQueryResponse(projectId, jobId, location, result));

            return Response.ok(job).build();
        } catch (SqlParseException e) {
            long endTime = Instant.now().toEpochMilli();
            JSONObject job = Fn.toBigQueryJob(projectId, jobId, location, query, null, startTime, endTime, e.getMessage());
            jobStorage.put(Fn.jobKey(projectId, jobId), job, null);

            Response.Status status = Response.Status.BAD_REQUEST;
            return Response.status(status)
                    .entity(new BigQueryError(e.getMessage(), status))
                    .build();
        } catch (RuntimeException e) {
            long endTime = Instant.now().toEpochMilli();
            JSONObject job = Fn.toBigQueryJob(projectId, jobId, location, query, null, startTime, endTime, e.getMessage());
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
        JSONObject job = jobStorage.jobDefinition(Fn.jobKey(projectId, jobId));
        if (job == null)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new BigQueryError("Not found: Job " + projectId + ":" + jobId, Response.Status.NOT_FOUND))
                    .build();

        return Response.ok(job).build();
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
}
