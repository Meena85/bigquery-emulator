package dev.meena.bigquery.controllers;

import dev.meena.test.StartPostgresContainers;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/*
 * Test {@link DatasetController} endpoints
 */
@QuarkusTest
@QuarkusTestResource(StartPostgresContainers.class)
public class TestDatasetController {

    private static final String projectId = "test";

    @Test
    void listIncludesSeedDataset() {
        given().when().get("/bigquery/v2/projects/{p}/datasets", projectId)
                .then().statusCode(200)
                .body("kind", equalTo("bigquery#datasetList"))
                .body("datasets.datasetReference.datasetId", hasItem("public"));
    }

    @Test
    void getUnknownDatasetReturnsNotFound() {
        given().when().get("/bigquery/v2/projects/{p}/datasets/{d}", projectId, "no_such_dataset")
                .then().statusCode(404)
                .body("error.status", equalTo("NOT_FOUND"))
                .body("error.message", equalTo("Not found: Dataset test:no_such_dataset"));
    }

    @Test
    void createGetAndDeleteDataset() {
        String id = "ds_" + System.nanoTime();
        String body = "{\"datasetReference\":{\"datasetId\":\"" + id + "\"},\"description\":\"hello\"}";

        given().contentType(ContentType.JSON).body(body)
                .when().post("/bigquery/v2/projects/{p}/datasets", projectId)
                .then().statusCode(200)
                .body("id", equalTo("test:" + id))
                .body("kind", equalTo("bigquery#dataset"))
                .body("description", equalTo("hello"));

        given().contentType(ContentType.JSON).body(body)
                .when().post("/bigquery/v2/projects/{p}/datasets", projectId)
                .then().statusCode(409).body("error.status", equalTo("ALREADY_EXISTS"));

        given().when().get("/bigquery/v2/projects/{p}/datasets/{d}", projectId, id)
                .then().statusCode(200).body("datasetReference.datasetId", equalTo(id));

        given().when().delete("/bigquery/v2/projects/{p}/datasets/{d}", projectId, id)
                .then().statusCode(204);

        given().when().get("/bigquery/v2/projects/{p}/datasets/{d}", projectId, id)
                .then().statusCode(404);
    }

    @Test
    void deleteNonEmptyDatasetRequiresDeleteContents() {
        given().when().delete("/bigquery/v2/projects/{p}/datasets/{d}", projectId, "public")
                .then().statusCode(400).body("error.errors[0].reason", equalTo("resourceInUse"));
    }

    @Test
    void createWithInvalidIdReturnsBadRequest() {
        given().contentType(ContentType.JSON).body("{\"datasetReference\":{\"datasetId\":\"bad-id!\"}}")
                .when().post("/bigquery/v2/projects/{p}/datasets", projectId)
                .then().statusCode(400);
    }
}
