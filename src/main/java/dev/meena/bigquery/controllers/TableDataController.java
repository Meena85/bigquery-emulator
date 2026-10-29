package dev.meena.bigquery.controllers;

import dev.meena.bigquery.db.Catalog;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.Bq2Psql;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.*;
import dev.meena.bigquery.types.BQType;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code .../tables/{tableId}/data} and {@code .../insertAll}: reading and streaming rows.
 */
@Path("/bigquery/v2/projects/{projectId}/datasets/{datasetId}/tables/{tableId}")
@Produces(MediaType.APPLICATION_JSON)
public class TableDataController {

    private static final Logger log = Logger.getLogger("TableDataController");
    private static final long DEFAULT_PAGE_SIZE = 5_000;

    @Inject
    Postgres postgres;

    @Inject
    Catalog catalog;

    @Inject
    Bq2Psql bq;

    @GET
    @Path("/data")
    public Response listData(@PathParam("projectId") String projectId,
                             @PathParam("datasetId") String datasetId,
                             @PathParam("tableId") String tableId,
                             @QueryParam("startIndex") Long startIndex,
                             @QueryParam("maxResults") Long maxResults,
                             @QueryParam("pageToken") String pageToken) {
        long offset = pageToken != null ? TableController.parseOffset(pageToken)
                : startIndex == null || startIndex < 0 ? 0 : startIndex;
        long limit = maxResults == null || maxResults <= 0 ? DEFAULT_PAGE_SIZE : maxResults;

        try {
            String schema = Fn.toSchema(projectId, datasetId);
            Catalog.TableInfo info = catalog.findTable(schema, tableId);
            if (info == null)
                return notFound(projectId, datasetId, tableId);

            String table = Fn.pgQuote(schema) + "." + Fn.pgQuote(info.name());
            long total = postgres.count("SELECT COUNT(*) FROM " + table);
            Postgres.QueryResult result = postgres.query("SELECT * FROM " + table + " OFFSET " + offset + " LIMIT " + limit);

            TableDataList response = BQType.tableDataList(result).totalRows(String.valueOf(total));
            if (offset + result.rows().size() < total)
                response.setPageToken(String.valueOf(offset + result.rows().size()));

            return Response.ok(response).build();
        } catch (RuntimeException e) {
            return Fn.toError(e);
        }
    }

    @POST
    @Path("/insertAll")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertAll(@PathParam("projectId") String projectId,
                              @PathParam("datasetId") String datasetId,
                              @PathParam("tableId") String tableId,
                              TableDataInsertAllRequest request) {
        if (request == null || request.getRows() == null)
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'rows' parameter is required");

        Catalog.TableInfo info = catalog.findTable(Fn.toSchema(projectId, datasetId), tableId);
        if (info == null)
            return notFound(projectId, datasetId, tableId);

        try {
            postgres.insertAll(bq.insertAll(request, projectId, datasetId, info.name()));
            return Response.ok(new TableDataInsertAllResponse()).build();
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Error inserting data into Postgres: " + e.getMessage(), e);

            // BigQuery reports row failures in the body of a 200 response.
            ErrorProto error = new ErrorProto().message(e.getMessage()).reason("invalid").location("rows[0]");
            return Response.ok(new TableDataInsertAllResponse().insertErrors(List.of(
                    new TableDataInsertAllResponseInsertErrorsInner().index(0).errors(List.of(error))))).build();
        }
    }

    private static Response notFound(String projectId, String datasetId, String tableId) {
        return Fn.bigQueryError(Response.Status.NOT_FOUND,
                "Not found: Table " + projectId + ":" + datasetId + "." + tableId);
    }
}
