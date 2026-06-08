package dev.meena.bigquery.controllers;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.*;
import dev.meena.test.StartPostgresContainers;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

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

    private static BigQuery bigQuery() {
        final BigQueryOptions.Builder builder = BigQueryOptions.newBuilder();
        if (StringUtils.isNotBlank(projectId)) {
            builder.setProjectId(projectId);
        }
        return builder
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
    void testHelloEndpoint() {
        given()
                .when().get("/")
                .then()
                .statusCode(200)
                .body(containsString("Query"));
    }

    @Test
    void query() throws InterruptedException {
        var bq = bigQuery();

        String sql = "SELECT * FROM `public`.`integration_test`";
        TableResult result = bq.query(wrapInConfig(sql));
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals("item 1", row.get(1).getStringValue());
    }

    @Test
    void createJob() throws InterruptedException {
        var bq = bigQuery();

        String query = "SELECT * FROM `public`.`integration_test`";
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
                .addRow("id", Map.of("value", "insert-test-1", "date", "2020-01-01", "data", "{ \"a\": \"test\" }"))
                .addRow("id", Map.of("value", "insert-test-2"))
                .build();
        InsertAllResponse insertResponse = bq.insertAll(request);
        Assertions.assertFalse(insertResponse.hasErrors());

        TableResult result = bq.query(wrapInConfig("SELECT COUNT(1) FROM `public`.`integration_test`"));
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals(5L, row.getFirst().getLongValue());
    }
}
