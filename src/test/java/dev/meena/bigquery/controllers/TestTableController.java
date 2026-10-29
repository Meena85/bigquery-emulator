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

import io.restassured.http.ContentType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(StartPostgresContainers.class)
public class TestTableController {

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

    @Test
    void getTableUnknownReturnsBigQueryNotFound() {
        given().when().get("/bigquery/v2/projects/{p}/datasets/{d}/tables/{t}", projectId, datasetId, "nope")
                .then().statusCode(404)
                .body("error.code", equalTo(404))
                .body("error.status", equalTo("NOT_FOUND"))
                .body("error.errors[0].reason", equalTo("notFound"));
    }

    @Test
    void listTablesIncludesSeedTable() {
        given().when().get("/bigquery/v2/projects/{p}/datasets/{d}/tables", projectId, datasetId)
                .then().statusCode(200)
                .body("kind", equalTo("bigquery#tableList"))
                .body("tables.tableReference.tableId", hasItem("integration_test"));
    }

    @Test
    void createListAndDeleteTable() throws InterruptedException {
        var bq = bigQuery();
        String name = "created_" + System.nanoTime();
        Schema schema = Schema.of(
                Field.newBuilder("id", StandardSQLTypeName.INT64).setMode(Field.Mode.REQUIRED).build(),
                Field.of("label", StandardSQLTypeName.STRING));
        bq.create(TableInfo.of(TableId.of(projectId, datasetId, name), StandardTableDefinition.of(schema)));

        Table table = bq.getTable(TableId.of(projectId, datasetId, name));
        Assertions.assertNotNull(table);
        Assertions.assertEquals(2, table.getDefinition().getSchema().getFields().size());
        Assertions.assertEquals(Field.Mode.REQUIRED, table.getDefinition().getSchema().getFields().get("id").getMode());

        Assertions.assertTrue(bq.delete(TableId.of(projectId, datasetId, name)));
        Assertions.assertNull(bq.getTable(TableId.of(projectId, datasetId, name)));
    }

    @Test
    void createExistingTableReturnsConflict() {
        given().contentType(ContentType.JSON)
                .body("{\"tableReference\":{\"tableId\":\"integration_test\"},\"schema\":{\"fields\":[{\"name\":\"a\",\"type\":\"STRING\"}]}}")
                .when().post("/bigquery/v2/projects/{p}/datasets/{d}/tables", projectId, datasetId)
                .then().statusCode(409).body("error.status", equalTo("ALREADY_EXISTS"));
    }

    @Test
    void tableDataPagesWithTotalRows() {
        given().queryParam("maxResults", 1)
                .when().get("/bigquery/v2/projects/{p}/datasets/{d}/tables/integration_test/data", projectId, datasetId)
                .then().statusCode(200)
                .body("rows.size()", equalTo(1))
                .body("totalRows", notNullValue())
                .body("pageToken", equalTo("1"));
    }

    @Test
    void insertAllIntoUnknownTableReturnsNotFound() {
        given().contentType(ContentType.JSON).body("{\"rows\":[{\"json\":{\"a\":1}}]}")
                .when().post("/bigquery/v2/projects/{p}/datasets/{d}/tables/nope/insertAll", projectId, datasetId)
                .then().statusCode(404);
    }

    @Test
    void unknownEndpointReturnsNotImplemented() {
        given().when().get("/bigquery/v2/projects/{p}/routines", projectId)
                .then().statusCode(501).body("path", equalTo("/bigquery/v2/projects/test/routines"));
    }

    @Test
    void patchAndPutTableMetadataAndAddColumns() {
        String name = "upd_" + System.nanoTime();
        String base = "/bigquery/v2/projects/{p}/datasets/{d}/tables";
        given().contentType(ContentType.JSON)
                .body("{\"tableReference\":{\"tableId\":\"" + name + "\"},\"description\":\"d1\",\"friendlyName\":\"F\","
                        + "\"labels\":{\"x\":\"1\"},\"schema\":{\"fields\":[{\"name\":\"a\",\"type\":\"STRING\"}]}}")
                .when().post(base, projectId, datasetId).then().statusCode(200).body("description", equalTo("d1"));

        given().contentType(ContentType.JSON)
                .body("{\"description\":\"d2\",\"schema\":{\"fields\":[{\"name\":\"a\",\"type\":\"STRING\"},{\"name\":\"B\",\"type\":\"INT64\"}]}}")
                .when().patch(base + "/{t}", projectId, datasetId, name)
                .then().statusCode(200)
                .body("description", equalTo("d2"))
                .body("friendlyName", equalTo("F"))
                .body("labels.x", equalTo("1"))
                .body("schema.fields.name", contains("a", "b"))
                .body("schema.fields.type", contains("STRING", "INTEGER"));

        given().contentType(ContentType.JSON).body("{\"friendlyName\":\"G\"}")
                .when().put(base + "/{t}", projectId, datasetId, name)
                .then().statusCode(200)
                .body("friendlyName", equalTo("G"))
                .body("description", nullValue());

        given().when().get(base + "/{t}", projectId, datasetId, name).then().body("friendlyName", equalTo("G"));
        given().when().delete(base + "/{t}", projectId, datasetId, name).then().statusCode(204);
    }

    @Test
    void updateUnknownTableReturnsNotFound() {
        given().contentType(ContentType.JSON).body("{\"description\":\"x\"}")
                .when().patch("/bigquery/v2/projects/{p}/datasets/{d}/tables/nope", projectId, datasetId).then().statusCode(404);
        given().contentType(ContentType.JSON).body("{\"description\":\"x\"}")
                .when().put("/bigquery/v2/projects/{p}/datasets/{d}/tables/nope", projectId, datasetId).then().statusCode(404);
    }
}
