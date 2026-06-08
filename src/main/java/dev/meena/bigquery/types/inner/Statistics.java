package dev.meena.bigquery.types.inner;

import java.util.List;

public record Statistics(
        long creationTime,
        long startTime,
        long endTime,
        Query query) {

    record Query(
            boolean cacheHit,
            String totalRows,
            String totalBytesBilled,
            String totalBytesProcessed) {
    }

    record Schema(List<Field> fields) {
    }

    record Field(
            String mode,
            String name,
            String type
    ) {
    }
}
