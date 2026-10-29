package dev.meena.bigquery.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.HashMap;
import java.util.Map;

/**
 * Dataset and table metadata Postgres has no column for. It is kept as JSON in the schema's or table's
 * comment; a comment that is not JSON (e.g. one written by hand) is read as the description.
 */
@RegisterForReflection
public record Metadata(String description, String friendlyName, Map<String, String> labels) {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static final Metadata EMPTY = new Metadata(null, null, Map.of());

    public Metadata {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }

    /** PUT semantics: the request replaces everything. */
    public static Metadata replace(String description, String friendlyName, Map<String, String> labels) {
        return new Metadata(description, friendlyName, labels);
    }

    /** PATCH semantics: fields left out keep their value; a label set to {@code null} is removed. */
    public Metadata patch(String description, String friendlyName, Map<String, String> labels) {
        Map<String, String> merged = new HashMap<>(this.labels);
        if (labels != null) {
            labels.forEach((key, value) -> {
                if (value == null) merged.remove(key);
                else merged.put(key, value);
            });
        }
        return new Metadata(description != null ? description : this.description,
                friendlyName != null ? friendlyName : this.friendlyName, merged);
    }

    public String toComment() {
        try {
            return JSON.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize metadata: " + e.getMessage(), e);
        }
    }

    public static Metadata fromComment(String comment) {
        if (comment == null || comment.isBlank())
            return EMPTY;

        try {
            if (comment.stripLeading().startsWith("{"))
                return JSON.readValue(comment, Metadata.class);
        } catch (Exception ignored) {
            // not ours: fall through and treat it as plain text
        }
        return new Metadata(comment, null, Map.of());
    }
}
