package dev.meena.bigquery.types;

public record JobKey(String projectId, String jobId) {

    @Override
    public String toString() {
        return JobKey.of(projectId, jobId);
    }

    public static String of(String projectId, String jobId) {
        if (projectId == null || jobId == null) {
            return null;
        }
        return projectId + ":" + jobId;
    }
}
