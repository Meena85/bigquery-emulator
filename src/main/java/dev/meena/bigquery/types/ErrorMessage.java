package dev.meena.bigquery.types;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ErrorMessage(String message) {
}
