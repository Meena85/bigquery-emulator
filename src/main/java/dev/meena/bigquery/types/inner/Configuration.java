package dev.meena.bigquery.types.inner;

public record Configuration(Query query) {

    public record Query(
            Table destinationTable,
            String query,
            boolean useLegacySql) {
    }

    public record Table(
            String datasetId,
            String tableId,
            String projectId) {
    }
}
