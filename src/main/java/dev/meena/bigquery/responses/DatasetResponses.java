package dev.meena.bigquery.responses;

import dev.meena.bigquery.db.Metadata;
import dev.meena.bigquery.model.Dataset;
import dev.meena.bigquery.model.DatasetList;
import dev.meena.bigquery.model.DatasetListDatasetsInner;
import dev.meena.bigquery.model.DatasetReference;

import java.util.List;

/**
 * Builds dataset payloads.
 */
public final class DatasetResponses {

    private static final String LOCATION = "US";

    private DatasetResponses() {
    }

    public static DatasetReference reference(String projectId, String datasetId) {
        return new DatasetReference().projectId(projectId).datasetId(datasetId);
    }

    public static Dataset dataset(String projectId, String datasetId, Metadata metadata) {
        String now = String.valueOf(System.currentTimeMillis());
        Dataset dataset = new Dataset(now, "\"" + projectId + ":" + datasetId + "\"", projectId + ":" + datasetId,
                "bigquery#dataset", now, null, null,
                "/bigquery/v2/projects/" + projectId + "/datasets/" + datasetId, null, "DEFAULT");
        dataset.setDatasetReference(reference(projectId, datasetId));
        dataset.setLocation(LOCATION);
        dataset.setDescription(metadata.description());
        dataset.setFriendlyName(metadata.friendlyName());
        dataset.setLabels(metadata.labels());
        return dataset;
    }

    public static DatasetListDatasetsInner datasetListItem(String projectId, String datasetId) {
        return new DatasetListDatasetsInner()
                .id(projectId + ":" + datasetId)
                .kind("bigquery#dataset")
                .location(LOCATION)
                .datasetReference(reference(projectId, datasetId));
    }

    public static DatasetList datasetList(String projectId, List<DatasetListDatasetsInner> datasets) {
        return new DatasetList("\"" + projectId + ":datasets\"", "bigquery#datasetList").datasets(datasets);
    }
}
