package dev.meena.bigquery.controllers;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.Bq2Psql;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.jobs.Disposition;
import dev.meena.bigquery.jobs.PgJobStorage;
import dev.meena.bigquery.model.Job;
import dev.meena.bigquery.model.JobConfigurationQuery;
import dev.meena.bigquery.model.QueryRequest;
import dev.meena.bigquery.model.TableReference;
import dev.meena.bigquery.types.BQType;
import dev.meena.bigquery.types.BigQueryError;
import dev.meena.bigquery.types.ErrorMessage;
import dev.meena.bigquery.types.JobBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Path("/bigquery/v2/")
public class JobsController {

    @Inject
    Postgres postgres;

    @Inject
    PgJobStorage jobStorage;

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
            return Response.ok(Fn.queryResultsResponse(projectId, "-", "US", result)).build();
        } catch (SqlParseException e) {
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, e.getMessage());
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
        var result = jobStorage.jobResults(projectId, jobId);
        if (result == null)
            return Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Job " + jobId + ", project=" + projectId);

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
            Job request) {

        if (request == null || request.getConfiguration() == null || request.getConfiguration().getQuery() == null)
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'configuration.query' parameter is required");

        JobConfigurationQuery jobQuery = request.getConfiguration().getQuery();

        String query = jobQuery.getQuery();
        if (query == null || query.isBlank())
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'configuration.query.query' parameter is required");

        long startTime = Instant.now().toEpochMilli();

        String jobId = null;
        if (request.getJobReference() != null && request.getJobReference().getJobId() != null)
            jobId = request.getJobReference().getJobId();

        if (isBlank(jobId))
            jobId = "job_" + UUID.randomUUID().toString().replace("-", "");

        String location = null;
        if (request.getJobReference() != null && request.getJobReference().getLocation() != null)
            location = request.getJobReference().getJobId();

        Disposition writeDisposition = Disposition.from(jobQuery.getWriteDisposition(), Disposition.WRITE_EMPTY);
        Disposition createDisposition = Disposition.from(jobQuery.getCreateDisposition(), Disposition.CREATE_IF_NEEDED);

        TableReference destinationTable = BQType.tableRefForJob(jobQuery.getDestinationTable(), jobId);

        JobBuilder jb = new JobBuilder(projectId, jobId)
                .location(location)
                .query(query)
                .destinationTable(destinationTable)
                .startTime(startTime)
                .writeDisposition(writeDisposition)
                .createDisposition(createDisposition);

        try {
            jobStorage.materializeQueryResult(destinationTable, bq.translate(query, projectId), writeDisposition, createDisposition);

            Postgres.QueryResult result = jobStorage.selectAll(destinationTable);
            if (result == null)
                result = new Postgres.QueryResult(List.of(), List.of());

            Job job = jb.result(result).build();
            jobStorage.put(projectId, jobId, job);
            return Response.ok(job).build();
        } catch (Exception e) {
            Job job = jb.errorMessage(e.getMessage()).build();
            jobStorage.put(projectId, jobId, job, new ErrorMessage(e.getMessage()));
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
        Job job = jobStorage.jobDefinition(projectId, jobId);
        if (job == null)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new BigQueryError("Not found: Job " + projectId + ":" + jobId, Response.Status.NOT_FOUND))
                    .build();

        return Response.ok(job).build();
    }

    @DELETE
    @Path("/projects/{projectId}/jobs/{jobId}/delete")
    public Response deleteJob(
            @PathParam("projectId") String projectId,
            @PathParam("jobId") String jobId) {
        jobStorage.delete(projectId, jobId);
        return Response.ok().build();
    }
}
