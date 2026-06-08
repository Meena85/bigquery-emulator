package dev.meena.bigquery.types.inner;

public record JobReference(String projectId,
                           String jobId,
                           String location) {
}
