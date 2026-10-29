package dev.meena.bigquery.jobs;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ErrorMessage(String message) {
}
