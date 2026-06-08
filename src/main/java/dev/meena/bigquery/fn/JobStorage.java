package dev.meena.bigquery.fn;

import dev.meena.bigquery.model.GetQueryResultsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class JobStorage {

    private final ConcurrentMap<String, JobItem> JOBS;

    record JobItem(JSONObject jobDefinition, GetQueryResultsResponse jobResults) {
    }

    public JobStorage() {
        JOBS = new ConcurrentHashMap<>();
    }

    public void put(String jobId, JSONObject jobDefinition, GetQueryResultsResponse jobResults) {
        JOBS.put(jobId, new JobItem(jobDefinition, jobResults));
    }

    public JSONObject jobDefinition(String jobId) {
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
