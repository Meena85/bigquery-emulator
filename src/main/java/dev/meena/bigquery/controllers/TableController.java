package dev.meena.bigquery.controllers;

import dev.meena.bigquery.responses.TableResponses;
import dev.meena.bigquery.responses.Errors;
import dev.meena.bigquery.db.Catalog;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.BqTypes;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * {@code /bigquery/v2/projects/{projectId}/datasets/{datasetId}/tables}: table metadata.
 * Row access ({@code data}, {@code insertAll}) lives in {@link TableDataController}.
 */
@Path("/bigquery/v2/projects/{projectId}/datasets/{datasetId}/tables")
@Produces(MediaType.APPLICATION_JSON)
public class TableController {

    private static final int DEFAULT_PAGE_SIZE = 1_000;

    @Inject
    Postgres postgres;

    @Inject
    Catalog catalog;

    @GET
    public Response listTables(@PathParam("projectId") String projectId,
                               @PathParam("datasetId") String datasetId,
                               @QueryParam("maxResults") Integer maxResults,
                               @QueryParam("pageToken") String pageToken) {
        long offset = parseOffset(pageToken);
        int limit = maxResults == null || maxResults <= 0 ? DEFAULT_PAGE_SIZE : maxResults;
        String schema = Fn.toSchema(projectId, datasetId);

        try {
            if (!catalog.schemaExists(schema))
                return Errors.bigQueryError(Response.Status.NOT_FOUND, "Not found: Dataset " + projectId + ":" + datasetId);

            Catalog.TablePage page = catalog.listTables(schema, offset, limit);
            List<TableListTablesInner> tables = page.tables().stream()
                    .map(t -> TableResponses.tableListItem(projectId, datasetId, t))
                    .toList();

            TableList response = TableResponses.tableList(projectId, datasetId, tables, page.total());
            if (offset + tables.size() < page.total())
                response.setNextPageToken(String.valueOf(offset + tables.size()));

            return Response.ok(response).build();
        } catch (RuntimeException e) {
            return Errors.toError(e);
        }
    }

    @GET
    @Path("/{tableId}")
    public Response getTable(@PathParam("projectId") String projectId,
                             @PathParam("datasetId") String datasetId,
                             @PathParam("tableId") String tableId,
                             @QueryParam("selectedFields") String selectedFields) {
        try {
            String schema = Fn.toSchema(projectId, datasetId);
            Catalog.TableInfo info = catalog.findTable(schema, tableId);
            if (info == null)
                return Errors.bigQueryError(Response.Status.NOT_FOUND,
                        "Not found: Table " + projectId + ":" + datasetId + "." + tableId);

            return Response.ok(describe(projectId, datasetId, tableId, info, selectedFields)).build();
        } catch (RuntimeException e) {
            return Errors.toError(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertTable(@PathParam("projectId") String projectId,
                                @PathParam("datasetId") String datasetId,
                                Table request) {
        String tableId = request == null || request.getTableReference() == null
                ? null
                : request.getTableReference().getTableId();
        if (isBlank(tableId))
            return Errors.bigQueryError(Response.Status.BAD_REQUEST, "'tableReference.tableId' parameter is required");

        List<TableFieldSchema> fields = request.getSchema() == null ? List.of() : request.getSchema().getFields();
        if (fields == null || fields.isEmpty())
            return Errors.bigQueryError(Response.Status.BAD_REQUEST, "'schema.fields' parameter is required");

        try {
            String schema = Fn.toSchema(projectId, datasetId);
            if (!catalog.schemaExists(schema))
                return Errors.bigQueryError(Response.Status.NOT_FOUND, "Not found: Dataset " + projectId + ":" + datasetId);
            if (catalog.findTable(schema, tableId) != null)
                return Errors.bigQueryError(Response.Status.CONFLICT,
                        "Already Exists: Table " + projectId + ":" + datasetId + "." + tableId);

            String columns = fields.stream()
                    .map(f -> Fn.pgQuote(StringUtils.lowerCase(f.getName())) + " " + toPgType(f)
                            + ("REQUIRED".equalsIgnoreCase(f.getMode()) ? " NOT NULL" : ""))
                    .collect(Collectors.joining(", "));
            postgres.execute("CREATE TABLE " + Fn.pgQuote(schema) + "." + Fn.pgQuote(tableId) + " (" + columns + ")");

            Catalog.TableInfo info = catalog.findTable(schema, tableId);
            return Response.ok(describe(projectId, datasetId, tableId, info, null)).build();
        } catch (RuntimeException e) {
            return Errors.toError(e);
        }
    }

    @DELETE
    @Path("/{tableId}")
    public Response deleteTable(@PathParam("projectId") String projectId,
                                @PathParam("datasetId") String datasetId,
                                @PathParam("tableId") String tableId) {
        try {
            String schema = Fn.toSchema(projectId, datasetId);
            Catalog.TableInfo info = catalog.findTable(schema, tableId);
            if (info == null)
                return Errors.bigQueryError(Response.Status.NOT_FOUND,
                        "Not found: Table " + projectId + ":" + datasetId + "." + tableId);

            postgres.execute("DROP " + (info.view() ? "VIEW " : "TABLE ") + Fn.pgQuote(schema) + "." + Fn.pgQuote(info.name()));
            return Response.noContent().build();
        } catch (RuntimeException e) {
            return Errors.toError(e);
        }
    }

    private Table describe(String projectId, String datasetId, String tableId, Catalog.TableInfo info, String selectedFields) {
        String schema = Fn.toSchema(projectId, datasetId);
        Set<String> selected = isBlank(selectedFields)
                ? null
                : java.util.Arrays.stream(selectedFields.split(",")).map(s -> s.trim().toLowerCase()).collect(Collectors.toSet());

        long numRows = postgres.count("SELECT COUNT(*) FROM " + Fn.pgQuote(schema) + "." + Fn.pgQuote(info.name()));
        return TableResponses.table(projectId, datasetId, tableId, info, catalog.columns(schema, info.name()), selected, numRows);
    }

    private static String toPgType(TableFieldSchema field) {
        String bqType = field.getType();
        if ("RANGE".equalsIgnoreCase(bqType) && field.getRangeElementType() != null)
            bqType = "RANGE<" + field.getRangeElementType().getType() + ">";

        String type = BqTypes.toPg(bqType);
        return "REPEATED".equalsIgnoreCase(field.getMode()) ? type + "[]" : type;
    }

    static long parseOffset(String pageToken) {
        if (isBlank(pageToken))
            return 0;

        try {
            return Math.max(0, Long.parseLong(pageToken));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
