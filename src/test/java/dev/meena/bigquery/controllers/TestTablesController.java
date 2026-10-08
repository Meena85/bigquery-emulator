package dev.meena.bigquery.controllers;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.*;
import dev.meena.bigquery.db.Postgres;
import dev.meena.test.StartPostgresContainers;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/*
 * TODO:

 * delete(TableId.of())
 * getTable(TableId.of())
 *
 * create(JobInfo.of(jobId, queryBuilder))
 * create(Job.of(config))
 * create(TableInfo.newBuilder()
 */
@QuarkusTest
@QuarkusTestResource(StartPostgresContainers.class)
public class TestTablesController {

    private static final String projectId = "test";
    private static final String datasetId = "public";

    private static BigQuery bigQuery() {
        return BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setHost("http://localhost:" + RestAssured.port)
                .setLocation("US")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    private static @NonNull QueryJobConfiguration wrapInConfig(@SuppressWarnings("SameParameterValue") String query) {
        return QueryJobConfiguration.newBuilder(query).build();
    }

    @Test
    void insert() throws InterruptedException {
        var bq = bigQuery();
        InsertAllRequest request = InsertAllRequest.newBuilder("public", "integration_test")
                .addRow("id", Map.of("text", "insert-test-1", "date", "2020-01-01", "data", "{ \"a\": \"test\" }"))
                .addRow("id", Map.of("text", "insert-test-2", "date", "2020-01-01", "data", "{ \"a\": \"test\" }"))
                .build();
        InsertAllResponse insertResponse = bq.insertAll(request);
        Assertions.assertFalse(insertResponse.hasErrors());

        TableResult result = bq.query(wrapInConfig("SELECT COUNT(1) FROM `public`.`integration_test`"));
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals(5L, row.getFirst().getLongValue());
    }

    @Test
    void getTableCaseInsensitive() {
        var bq = bigQuery();
        Table table = bq.getTable(TableId.of(projectId, datasetId, "INTEGRATION_TEST"));
        Assertions.assertNotNull(table);
        Assertions.assertEquals("INTEGRATION_TEST", table.getTableId().getTable());
        Assertions.assertEquals(TableDefinition.Type.TABLE, table.getDefinition().getType());
    }
}
