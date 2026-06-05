package dev.meena.types;

import dev.meena.db.Postgres;
import dev.meena.types.inner.Configuration;
import dev.meena.types.inner.JobReference;

public class JobResponse extends Response {

    public static JobResponse from(
            String projectId,
            String jobId,
            String location,
            String query,
            Postgres.QueryResult result,
            long startTime,
            long endTime,
            String errorMessage
    ) {
        String dataset = "public";
        String tableId = "integration_test";

        JobResponse j = new JobResponse();
        j.kind = "bigquery#job";
        j.etag = "\"" + jobId + "\"";
        j.id = projectId + ":" + location + "." + jobId;
        j.selfLink = "/bigquery/v2/projects/" + projectId + "/jobs/" + jobId;

        j.jobReference = new JobReference(projectId, jobId, location);
        j.configuration = new Configuration(new Configuration.Query(new Configuration.Table(dataset, tableId, projectId), query, false));
        return j;
    }
}