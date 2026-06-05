package dev.meena.controllers;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.*;
import dev.meena.controllers.fn.Fn;
import dev.meena.db.Postgres;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

import java.util.List;

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
 *
 * insertAll(InsertAllRequest.newBuilder())
 */
@QuarkusTest
public class TestMainController {

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
        QueryJobConfiguration config =
                QueryJobConfiguration.newBuilder(sql).build();
        TableResult result = bq.query(config);
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals("item 1", row.get(1).getStringValue());
    }

    @Test
    void createJobInfo() throws InterruptedException {
        var bq = bigQuery();

        QueryJobConfiguration config =
                QueryJobConfiguration.newBuilder("SELECT * FROM `public`.`integration_test`").build();
        Job job = bq.create(JobInfo.newBuilder(config).build());

        job.waitFor();

        TableResult result = job.getQueryResults();
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals("item 1", row.get(1).getStringValue());
    }

    @Test
    void out() {
        JSONObject out = Fn.toBigQueryJob("PROJECT_ID",
                "JOB_ID",
                "LOCATION",
                "SELECT * FROM QUERY;",
                new Postgres.QueryResult(
                        List.of(new Postgres.Column("column_1", 7, "string")),
                        List.of(List.of("row value 1"))),
                1000000L,
                2000000L,
                "ERROR MSG");

        System.out.println("BigQuery: " + out.toString());
    }
}
