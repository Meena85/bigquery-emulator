package dev.meena.bigquery.controllers;

import dev.meena.bigquery.dialect.Bq2Psql;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.jobs.Disposition;
import dev.meena.bigquery.jobs.PgJobStorage;
import dev.meena.bigquery.model.Job;
import dev.meena.bigquery.model.JobConfigurationQuery;
import dev.meena.bigquery.model.JobReference;
import dev.meena.bigquery.model.TableReference;
import dev.meena.bigquery.types.BQType;
import dev.meena.bigquery.types.ErrorMessage;
import dev.meena.bigquery.types.JobBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * {@code /bigquery/v2/projects/{projectId}/jobs}: query job creation, lookup and deletion.
 */
@Path("/bigquery/v2/projects/{projectId}/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class JobController {

    @Inject
    PgJobStorage jobStorage;

    @Inject
    Bq2Psql bq;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createJob(@PathParam("projectId") String projectId, Job request) {
        if (request == null || request.getConfiguration() == null || request.getConfiguration().getQuery() == null)
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'configuration.query' parameter is required");

        JobConfigurationQuery jobQuery = request.getConfiguration().getQuery();
        String query = jobQuery.getQuery();
        if (isBlank(query))
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'configuration.query.query' parameter is required");

        JobReference ref = request.getJobReference();
        String jobId = ref != null && !isBlank(ref.getJobId())
                ? ref.getJobId()
                : "job_" + UUID.randomUUID().toString().replace("-", "");
        String location = ref == null ? null : ref.getLocation();

        Disposition writeDisposition = Disposition.from(jobQuery.getWriteDisposition(), Disposition.WRITE_EMPTY);
        Disposition createDisposition = Disposition.from(jobQuery.getCreateDisposition(), Disposition.CREATE_IF_NEEDED);
        TableReference destinationTable = BQType.tableRefForJob(jobQuery.getDestinationTable(), jobId);

        JobBuilder jb = new JobBuilder(projectId, jobId)
                .location(location)
                .query(query)
                .destinationTable(destinationTable)
                .startTime(Instant.now().toEpochMilli())
                .writeDisposition(writeDisposition)
                .createDisposition(createDisposition);

        try {
            jobStorage.materializeQueryResult(destinationTable, bq.translate(query, projectId), writeDisposition, createDisposition);

            Postgres.QueryResult result = jobStorage.selectAll(destinationTable);
            Job job = jb.result(result == null ? new Postgres.QueryResult(List.of(), List.of()) : result).build();
            jobStorage.put(projectId, jobId, job);
            return Response.ok(job).build();
        } catch (Exception e) {
            jobStorage.put(projectId, jobId, jb.errorMessage(e.getMessage()).build(), new ErrorMessage(e.getMessage()));
            return Fn.toError(e);
        }
    }

    @GET
    @Path("/{jobId}")
    public Response getJob(@PathParam("projectId") String projectId, @PathParam("jobId") String jobId) {
        Job job = jobStorage.jobDefinition(projectId, jobId);
        if (job == null)
            return Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Job " + projectId + ":" + jobId);

        return Response.ok(job).build();
    }

    @DELETE
    @Path("/{jobId}/delete")
    public Response deleteJob(@PathParam("projectId") String projectId, @PathParam("jobId") String jobId) {
        jobStorage.delete(projectId, jobId);
        return Response.noContent().build();
    }
}
