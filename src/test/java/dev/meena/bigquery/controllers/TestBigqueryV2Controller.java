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
public class TestBigqueryV2Controller {

    private static final String projectId = "test";
    private static final String datasetId = "public";

    @Inject
    Postgres postgres;

    private static BigQuery bigQuery() {
        return BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setHost("http://localhost:" + RestAssured.port)
                .setLocation("US")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    private static @NonNull QueryJobConfiguration wrapInConfig(String query) {
        return QueryJobConfiguration.newBuilder(query).build();
    }

    @Test
    void query() throws InterruptedException {
        var bq = bigQuery();

        String sql = "SELECT * FROM `public`.integration_test";
        TableResult result = bq.query(wrapInConfig(sql));
        FieldValueList row = result.iterateAll().iterator().next();

        postgres.logTablesAndColumns();
        Assertions.assertEquals("item 1", row.get(1).getStringValue());
    }

    @Test
    void queryWithProject() throws InterruptedException {
        var bq = bigQuery();

        String sql = "SELECT * FROM `test`.`public`.integration_test";
        TableResult result = bq.query(wrapInConfig(sql));
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals("item 1", row.get(1).getStringValue());
    }

    @Test
    void createJob() throws InterruptedException {
        var bq = bigQuery();

        String query = "SELECT * FROM `public`.integration_test";
        Job job = bq.create(JobInfo.newBuilder(wrapInConfig(query)).build());

        job.waitFor();

        TableResult result = job.getQueryResults();
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals("item 1", row.get(1).getStringValue());
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
}
