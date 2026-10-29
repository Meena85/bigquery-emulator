package dev.meena.bigquery.controllers;

import dev.meena.bigquery.db.Catalog;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.Dataset;
import dev.meena.bigquery.model.DatasetList;
import dev.meena.bigquery.model.DatasetListDatasetsInner;
import dev.meena.bigquery.model.DatasetReference;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.regex.Pattern;

/**
 * {@code /bigquery/v2/projects/{projectId}/datasets}. A dataset is a Postgres schema named
 * {@code <project>$<dataset>} (see {@link Fn#toSchema}).
 */
@Path("/bigquery/v2/projects/{projectId}/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetController {

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_]{1,1024}");
    private static final String LOCATION = "US";

    @Inject
    Postgres postgres;

    @Inject
    Catalog catalog;

    @GET
    public Response listDatasets(@PathParam("projectId") String projectId) {
        String prefix = Fn.toSchema(projectId, "");
        List<DatasetListDatasetsInner> datasets = catalog.schemasWithPrefix(prefix).stream()
                .map(schema -> schema.substring(prefix.length()))
                .map(datasetId -> new DatasetListDatasetsInner()
                        .id(projectId + ":" + datasetId)
                        .kind("bigquery#dataset")
                        .location(LOCATION)
                        .datasetReference(reference(projectId, datasetId)))
                .toList();

        return Response.ok(new DatasetList("\"" + projectId + ":datasets\"", "bigquery#datasetList").datasets(datasets)).build();
    }

    @GET
    @Path("/{datasetId}")
    public Response getDataset(@PathParam("projectId") String projectId, @PathParam("datasetId") String datasetId) {
        if (!catalog.schemaExists(Fn.toSchema(projectId, datasetId)))
            return notFound(projectId, datasetId);

        return Response.ok(describe(projectId, datasetId, null)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertDataset(@PathParam("projectId") String projectId, Dataset request) {
        String datasetId = request == null || request.getDatasetReference() == null
                ? null
                : request.getDatasetReference().getDatasetId();
        if (datasetId == null || !VALID_ID.matcher(datasetId).matches())
            return Fn.bigQueryError(Response.Status.BAD_REQUEST,
                    "Invalid dataset ID \"" + datasetId + "\". Dataset IDs must be alphanumeric (plus underscores) and at most 1024 characters long.");

        String schema = Fn.toSchema(projectId, datasetId);
        if (catalog.schemaExists(schema))
            return Fn.bigQueryError(Response.Status.CONFLICT, "Already Exists: Dataset " + projectId + ":" + datasetId);

        try {
            postgres.execute("CREATE SCHEMA " + Fn.pgQuote(schema));
            return Response.ok(describe(projectId, datasetId, request)).build();
        } catch (RuntimeException e) {
            return Fn.toError(e);
        }
    }

    @DELETE
    @Path("/{datasetId}")
    public Response deleteDataset(@PathParam("projectId") String projectId,
                                  @PathParam("datasetId") String datasetId,
                                  @QueryParam("deleteContents") boolean deleteContents) {
        String schema = Fn.toSchema(projectId, datasetId);
        if (!catalog.schemaExists(schema))
            return notFound(projectId, datasetId);

        if (!deleteContents && catalog.schemaHasTables(schema))
            return Fn.bigQueryError(Response.Status.BAD_REQUEST,
                    "Dataset " + projectId + ":" + datasetId + " is still in use", "resourceInUse");

        try {
            postgres.execute("DROP SCHEMA " + Fn.pgQuote(schema) + " CASCADE");
            return Response.noContent().build();
        } catch (RuntimeException e) {
            return Fn.toError(e);
        }
    }

    private static Dataset describe(String projectId, String datasetId, Dataset requested) {
        String now = String.valueOf(System.currentTimeMillis());
        Dataset dataset = new Dataset(now, "\"" + projectId + ":" + datasetId + "\"", projectId + ":" + datasetId,
                "bigquery#dataset", now, null, null,
                "/bigquery/v2/projects/" + projectId + "/datasets/" + datasetId, null, "DEFAULT");
        dataset.setDatasetReference(reference(projectId, datasetId));
        dataset.setLocation(LOCATION);
        if (requested != null) {
            dataset.setDescription(requested.getDescription());
            dataset.setFriendlyName(requested.getFriendlyName());
        }
        return dataset;
    }

    private static DatasetReference reference(String projectId, String datasetId) {
        return new DatasetReference().projectId(projectId).datasetId(datasetId);
    }

    private static Response notFound(String projectId, String datasetId) {
        return Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Dataset " + projectId + ":" + datasetId);
    }
}
