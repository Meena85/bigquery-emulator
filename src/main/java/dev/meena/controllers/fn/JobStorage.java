package dev.meena.controllers.fn;

import jakarta.enterprise.context.ApplicationScoped;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class JobStorage {

    private final ConcurrentMap<String, JobItem> JOBS;

    record JobItem(JSONObject jobDefinition, JSONObject jobResults) {
    }

    public JobStorage() {
        JOBS = new ConcurrentHashMap<>();
    }

    public void put(String jobId, JSONObject jobDefinition, JSONObject jobResults) {
        JOBS.put(jobId, new JobItem(jobDefinition, jobResults));
    }

    public JSONObject jobDefinition(String jobId) {
        JobItem job = JOBS.get(jobId);
        return job == null ? null : job.jobDefinition();
    }

    public JSONObject jobResults(String jobId) {
        JobItem job = JOBS.get(jobId);
        return job == null ? null : job.jobResults();
    }

    public void remove(String jobId) {
        JOBS.remove(jobId);
    }
}
