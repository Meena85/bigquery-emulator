package dev.meena.bigquery.fn;

import dev.meena.bigquery.model.GetQueryResultsResponse;
import dev.meena.bigquery.model.Job;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class JobStorage {

    private final ConcurrentMap<String, JobItem> JOBS;

    record JobItem(Job jobDefinition, GetQueryResultsResponse jobResults) {
    }

    public JobStorage() {
        JOBS = new ConcurrentHashMap<>();
    }

    public void put(String jobId, Job jobDefinition, GetQueryResultsResponse jobResults) {
        JOBS.put(jobId, new JobItem(jobDefinition, jobResults));
    }

    public Job jobDefinition(String jobId) {
        JobItem job = JOBS.get(jobId);
        return job == null ? null : job.jobDefinition();
    }

    public GetQueryResultsResponse jobResults(String jobId) {
        JobItem job = JOBS.get(jobId);
        return job == null ? null : job.jobResults();
    }

    public void remove(String jobId) {
        JOBS.remove(jobId);
    }
}
