package dev.meena.bigquery.db;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetadataTest {

    @Test
    void roundTripsThroughComment() {
        Metadata metadata = new Metadata("it's a \"desc\"", "Name", Map.of("k", "v"));
        assertEquals(metadata, Metadata.fromComment(metadata.toComment()));
    }

    @Test
    void plainTextCommentIsTheDescription() {
        assertEquals("for tests", Metadata.fromComment("for tests").description());
        assertEquals("{ not json", Metadata.fromComment("{ not json").description());
    }

    @Test
    void missingCommentIsEmpty() {
        assertEquals(Metadata.EMPTY, Metadata.fromComment(null));
        assertEquals(Metadata.EMPTY, Metadata.fromComment("  "));
    }

    @Test
    void patchKeepsUntouchedFieldsAndRemovesNullLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("a", null);
        labels.put("c", "3");

        Metadata patched = new Metadata("d", "f", Map.of("a", "1", "b", "2")).patch(null, "g", labels);

        assertEquals("d", patched.description());
        assertEquals("g", patched.friendlyName());
        assertEquals(Map.of("b", "2", "c", "3"), patched.labels());
    }

    @Test
    void replaceDropsFieldsLeftOut() {
        Metadata replaced = Metadata.replace("d", null, null);
        assertEquals("d", replaced.description());
        assertNull(replaced.friendlyName());
        assertEquals(Map.of(), replaced.labels());
    }
}
