package dev.meena.bigquery.types;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.jobs.PgJobStorage;
import dev.meena.bigquery.model.*;

public class BQType {

    public static String etag(String projectId, String datasetId, String tableId) {
        return "\"" + projectId + ":" + datasetId + "." + tableId + "\"";
    }

    public static TableReference tableReference(String projectId, String datasetId, String tableName) {
        TableReference tableReference = new TableReference();
        tableReference.setProjectId(projectId);
        tableReference.setDatasetId(datasetId);
        tableReference.setTableId(tableName);
        return tableReference;
    }

    public static TableReference tableRefForJob(TableReference destinationTable, String jobID) {
        if (destinationTable == null)
            return tableReference(PgJobStorage.JOBS_PROJECT, PgJobStorage.JOBS_DATASET, jobID);

        if (destinationTable.getProjectId() == null)
            destinationTable.setProjectId(PgJobStorage.JOBS_PROJECT);

        if (destinationTable.getDatasetId() == null)
            destinationTable.setDatasetId(PgJobStorage.JOBS_DATASET);

        if (destinationTable.getTableId() == null)
            destinationTable.setTableId(jobID);

        return destinationTable;
    }

    public static TableDataList tableDataList(Postgres.QueryResult result) {
        return new TableDataList()
                .totalRows(String.valueOf(result.rows().size()))
                .rows(Fn.tableRows(result.rows()));
    }
}
