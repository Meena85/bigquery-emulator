package dev.meena.bigquery.controllers;

import dev.meena.bigquery.responses.Errors;
import dev.meena.bigquery.db.Catalog;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.Dataset;
import dev.meena.bigquery.responses.DatasetResponses;
import dev.meena.bigquery.model.DatasetListDatasetsInner;
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

    @Inject
    Postgres postgres;

    @Inject
    Catalog catalog;

    @GET
    public Response listDatasets(@PathParam("projectId") String projectId) {
        String prefix = Fn.toSchema(projectId, "");
        List<DatasetListDatasetsInner> datasets = catalog.schemasWithPrefix(prefix).stream()
                .map(schema -> DatasetResponses.datasetListItem(projectId, schema.substring(prefix.length())))
                .toList();

        return Response.ok(DatasetResponses.datasetList(projectId, datasets)).build();
    }

    @GET
    @Path("/{datasetId}")
    public Response getDataset(@PathParam("projectId") String projectId, @PathParam("datasetId") String datasetId) {
        if (!catalog.schemaExists(Fn.toSchema(projectId, datasetId)))
            return notFound(projectId, datasetId);

        return Response.ok(DatasetResponses.dataset(projectId, datasetId, null)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertDataset(@PathParam("projectId") String projectId, Dataset request) {
        String datasetId = request == null || request.getDatasetReference() == null
                ? null
                : request.getDatasetReference().getDatasetId();
        if (datasetId == null || !VALID_ID.matcher(datasetId).matches())
            return Errors.bigQueryError(Response.Status.BAD_REQUEST,
                    "Invalid dataset ID \"" + datasetId + "\". Dataset IDs must be alphanumeric (plus underscores) and at most 1024 characters long.");

        String schema = Fn.toSchema(projectId, datasetId);
        if (catalog.schemaExists(schema))
            return Errors.bigQueryError(Response.Status.CONFLICT, "Already Exists: Dataset " + projectId + ":" + datasetId);

        try {
            postgres.execute("CREATE SCHEMA " + Fn.pgQuote(schema));
            return Response.ok(DatasetResponses.dataset(projectId, datasetId, request)).build();
        } catch (RuntimeException e) {
            return Errors.toError(e);
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
            return Errors.bigQueryError(Response.Status.BAD_REQUEST,
                    "Dataset " + projectId + ":" + datasetId + " is still in use", "resourceInUse");

        try {
            postgres.execute("DROP SCHEMA " + Fn.pgQuote(schema) + " CASCADE");
            return Response.noContent().build();
        } catch (RuntimeException e) {
            return Errors.toError(e);
        }
    }

    private static Response notFound(String projectId, String datasetId) {
        return Errors.bigQueryError(Response.Status.NOT_FOUND, "Not found: Dataset " + projectId + ":" + datasetId);
    }
}
