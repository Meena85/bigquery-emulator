package dev.meena.bigquery.controllers;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.Bq2Psql;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.jobs.PgJobStorage;
import dev.meena.bigquery.model.QueryRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * {@code /bigquery/v2/projects/{projectId}/queries}: synchronous queries and query results.
 */
@Path("/bigquery/v2/projects/{projectId}/queries")
@Produces(MediaType.APPLICATION_JSON)
public class QueryController {

    private static final String DEFAULT_LOCATION = "US";

    @Inject
    Postgres postgres;

    @Inject
    PgJobStorage jobStorage;

    @Inject
    Bq2Psql bq;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response query(@PathParam("projectId") String projectId, QueryRequest request) {
        if (request == null || isBlank(request.getQuery()))
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'query' parameter is required");

        try {
            Postgres.QueryResult result = postgres.query(bq.translate(request.getQuery(), projectId));
            String jobId = "job_" + UUID.randomUUID().toString().replace("-", "");
            return Response.ok(Fn.queryResponse(projectId, jobId, DEFAULT_LOCATION, result)).build();
        } catch (Exception e) {
            return Fn.toError(e);
        }
    }

    @GET
    @Path("/{jobId}")
    public Response getQueryResults(@PathParam("projectId") String projectId, @PathParam("jobId") String jobId) {
        var result = jobStorage.jobResults(projectId, jobId);
        if (result == null)
            return Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Job " + projectId + ":" + jobId);

        return Response.ok(result).build();
    }

    /** Alias of {@link #getQueryResults} kept for clients that use the RPC-style path. */
    @GET
    @Path("/{jobId}/getQueryResults")
    public Response getQueryResultsRpc(@PathParam("projectId") String projectId, @PathParam("jobId") String jobId) {
        return getQueryResults(projectId, jobId);
    }
}
