package dev.meena.bigquery.controllers;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.*;
import dev.meena.test.StartPostgresContainers;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static io.restassured.RestAssured.given;

/*
 * Test {@link JobsController} endpoints
 */
@QuarkusTest
@QuarkusTestResource(StartPostgresContainers.class)
public class TestJobsController {

    private static final String projectId = "test";

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

    /*
     * Verifies BigQuery client query execution against the emulator for a dataset-qualified table.
     */
    @Test
    void query() throws InterruptedException {
        var bq = bigQuery();

        String sql = "SELECT * FROM `public`.integration_test";
        TableResult result = bq.query(wrapInConfig(sql));
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals("item 1", row.get(1).getStringValue());
    }

    /*
     * Verifies query translation and execution when the SQL includes project and dataset qualifiers.
     */
    @Test
    void queryWithProject() throws InterruptedException {
        var bq = bigQuery();

        String sql = "SELECT * FROM `test`.`public`.integration_test";
        TableResult result = bq.query(wrapInConfig(sql));
        FieldValueList row = result.iterateAll().iterator().next();

        Assertions.assertEquals("item 1", row.get(1).getStringValue());
    }

    /*
     * Verifies asynchronous job creation via BigQuery client and result retrieval through job polling.
     */
    @Test
    void createJob() throws InterruptedException {
        var bq = bigQuery();

        String query = "SELECT * FROM `public`.integration_test";
        Job job = bq.create(JobInfo.newBuilder(wrapInConfig(query)).build());

        job.waitFor();

        TableResult result = job.getQueryResults();
        String value = null;
        for (FieldValueList fieldValues : result.iterateAll()) {
            value = fieldValues.get(1).getStringValue();
            break;
        }

        Assertions.assertEquals("item 1", value);
    }

    /*
     * Verifies create job response and follow-up reads across jobs and query-results endpoints,
     * including persisted default disposition values.
     */
    @Test
    void createJobThenFetchViaJobsAndQueriesEndpoints() {
        String jobId = uniqueId("jobs-fetch");
        String payload = jobPayload(jobId, "SELECT * FROM `public`.integration_test");

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/bigquery/v2/projects/{projectId}/jobs", projectId)
                .then()
                .statusCode(200)
                .body("jobReference.jobId", Matchers.equalTo(jobId))
                .body("configuration.query.writeDisposition", Matchers.equalTo("WRITE_EMPTY"))
                .body("configuration.query.createDisposition", Matchers.equalTo("CREATE_IF_NEEDED"));

        given()
                .when()
                .get("/bigquery/v2/projects/{projectId}/jobs/{jobId}", projectId, jobId)
                .then()
                .statusCode(200)
                .body("jobReference.jobId", Matchers.equalTo(jobId))
                .body("configuration.query.writeDisposition", Matchers.equalTo("WRITE_EMPTY"))
                .body("configuration.query.createDisposition", Matchers.equalTo("CREATE_IF_NEEDED"));

        given()
                .when()
                .get("/bigquery/v2/projects/{projectId}/queries/{jobId}", projectId, jobId)
                .then()
                .statusCode(200)
                .body("jobReference.jobId", Matchers.equalTo(jobId))
                .body("totalRows", Matchers.notNullValue());

        given()
                .when()
                .get("/bigquery/v2/projects/{projectId}/queries/{jobId}/getQueryResults", projectId, jobId)
                .then()
                .statusCode(200)
                .body("jobReference.jobId", Matchers.equalTo(jobId));
    }

    /*
     * Verifies validation for missing query text in POST /queries requests.
     */
    @Test
    void queriesEndpointReturnsBadRequestWhenQueryIsMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/bigquery/v2/projects/{projectId}/queries", projectId)
                .then()
                .statusCode(400)
                .body("error.message", Matchers.equalTo("'query' parameter is required"));
    }

    /*
     * Verifies GET /jobs returns a BigQuery-style not-found error for an unknown job id.
     */
    @Test
    void getJobReturnsNotFoundForUnknownId() {
        String missingJobId = uniqueId("missing-job");
        given()
                .when()
                .get("/bigquery/v2/projects/{projectId}/jobs/{jobId}", projectId, missingJobId)
                .then()
                .statusCode(404)
                .body("error.message", Matchers.equalTo("Not found: Job " + projectId + ":" + missingJobId));
    }

    /*
     * Verifies GET /queries/{jobId} returns not found when no result exists for the given job id.
     */
    @Test
    void getQueryResultsReturnsNotFoundForUnknownId() {
        String missingJobId = uniqueId("missing-result");
        given()
                .when()
                .get("/bigquery/v2/projects/{projectId}/queries/{jobId}", projectId, missingJobId)
                .then()
                .statusCode(404)
                .body("error.message", Matchers.startsWith("Not found: Job " + missingJobId));
    }

    /*
     * Verifies create job endpoint supports gzip-compressed request payloads and applies defaults.
     */
    @Test
    void createJobAcceptsGzipEncodedRequestBody() throws IOException {
        String jobId = uniqueId("gzip-job");
        String payload = jobPayload(jobId, "SELECT * FROM `public`.integration_test");

        given()
                .contentType(ContentType.JSON)
                .header("Content-Encoding", "gzip")
                .body(gzip(payload))
                .when()
                .post("/bigquery/v2/projects/{projectId}/jobs", projectId)
                .then()
                .statusCode(200)
                .body("jobReference.jobId", Matchers.equalTo(jobId))
                .body("configuration.query.writeDisposition", Matchers.equalTo("WRITE_EMPTY"))
                .body("configuration.query.createDisposition", Matchers.equalTo("CREATE_IF_NEEDED"));
    }

    /*
     * Verifies destination table behavior for WRITE_TRUNCATE followed by WRITE_APPEND,
     * and confirms dispositions are echoed in job configuration.
     */
    @Test
    void createJobWithWriteAppendAddsRowsToDestinationTable() throws InterruptedException {
        String tableId = uniqueId("append_table");
        String truncateJobId = uniqueId("append-job-1");
        String appendJobId = uniqueId("append-job-2");

        given()
                .contentType(ContentType.JSON)
                .body(jobPayloadWithDestination(truncateJobId,
                        "SELECT * FROM `public`.integration_test WHERE text = 'item 1'",
                        tableId,
                        "WRITE_TRUNCATE"))
                .when()
                .post("/bigquery/v2/projects/{projectId}/jobs", projectId)
                .then()
                .statusCode(200)
                .body("jobReference.jobId", Matchers.equalTo(truncateJobId))
                .body("configuration.query.writeDisposition", Matchers.equalTo("WRITE_TRUNCATE"))
                .body("configuration.query.createDisposition", Matchers.equalTo("CREATE_IF_NEEDED"));

        given()
                .contentType(ContentType.JSON)
                .body(jobPayloadWithDestination(appendJobId,
                        "SELECT * FROM `public`.integration_test WHERE text = 'item 2'",
                        tableId,
                        "WRITE_APPEND"))
                .when()
                .post("/bigquery/v2/projects/{projectId}/jobs", projectId)
                .then()
                .statusCode(200)
                .body("jobReference.jobId", Matchers.equalTo(appendJobId))
                .body("configuration.query.writeDisposition", Matchers.equalTo("WRITE_APPEND"))
                .body("configuration.query.createDisposition", Matchers.equalTo("CREATE_IF_NEEDED"));

        TableResult countResult = bigQuery().query(wrapInConfig("SELECT COUNT(1) FROM `bq`.`public`.`" + tableId + "`"));
        long rowCount = 0;
        for (FieldValueList fieldValues : countResult.iterateAll()) {
            rowCount = fieldValues.getFirst().getLongValue();
            break;
        }
        Assertions.assertEquals(2L, rowCount);
    }

    private static String uniqueId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String jobPayload(String jobId, @SuppressWarnings("SameParameterValue") String query) {
        return """
                {
                  "jobReference": {
                    "jobId": "%s",
                    "location": "US"
                  },
                  "configuration": {
                    "query": {
                      "query": "%s",
                      "useLegacySql": false
                    }
                  }
                }
                """.formatted(jobId, query);
    }

    private static String jobPayloadWithDestination(String jobId, String query, String tableId, String writeDisposition) {
        return """
                {
                  "jobReference": {
                    "jobId": "%s",
                    "location": "US"
                  },
                  "configuration": {
                    "query": {
                      "query": "%s",
                      "useLegacySql": false,
                      "writeDisposition": "%s",
                      "destinationTable": {
                        "datasetId": "public",
                        "tableId": "%s"
                      }
                    }
                  }
                }
                """.formatted(jobId, query, writeDisposition, tableId);
    }

    private static byte[] gzip(String body) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return byteStream.toByteArray();
    }
}
