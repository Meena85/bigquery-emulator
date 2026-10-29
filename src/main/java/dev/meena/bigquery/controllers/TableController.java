package dev.meena.bigquery.controllers;

import dev.meena.bigquery.db.Catalog;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.BqTypes;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.*;
import dev.meena.bigquery.types.BQType;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
                return Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Dataset " + projectId + ":" + datasetId);

            Catalog.TablePage page = catalog.listTables(schema, offset, limit);
            List<TableListTablesInner> tables = page.tables().stream()
                    .map(t -> new TableListTablesInner()
                            .kind("bigquery#table")
                            .id(projectId + ":" + datasetId + "." + t.name())
                            .friendlyName(t.name())
                            .tableReference(BQType.tableReference(projectId, datasetId, t.name()))
                            .type(t.view() ? "VIEW" : "TABLE"))
                    .toList();

            TableList response = new TableList()
                    .kind("bigquery#tableList")
                    .etag("\"" + projectId + ":" + datasetId + ":tables\"")
                    .tables(tables)
                    .totalItems((int) page.total());
            if (offset + tables.size() < page.total())
                response.setNextPageToken(String.valueOf(offset + tables.size()));

            return Response.ok(response).build();
        } catch (RuntimeException e) {
            return Fn.toError(e);
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
                return Fn.bigQueryError(Response.Status.NOT_FOUND,
                        "Not found: Table " + projectId + ":" + datasetId + "." + tableId);

            return Response.ok(describe(projectId, datasetId, tableId, info, selectedFields)).build();
        } catch (RuntimeException e) {
            return Fn.toError(e);
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
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'tableReference.tableId' parameter is required");

        List<TableFieldSchema> fields = request.getSchema() == null ? List.of() : request.getSchema().getFields();
        if (fields == null || fields.isEmpty())
            return Fn.bigQueryError(Response.Status.BAD_REQUEST, "'schema.fields' parameter is required");

        try {
            String schema = Fn.toSchema(projectId, datasetId);
            if (!catalog.schemaExists(schema))
                return Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Dataset " + projectId + ":" + datasetId);
            if (catalog.findTable(schema, tableId) != null)
                return Fn.bigQueryError(Response.Status.CONFLICT,
                        "Already Exists: Table " + projectId + ":" + datasetId + "." + tableId);

            String columns = fields.stream()
                    .map(f -> Fn.pgQuote(f.getName().toLowerCase()) + " " + toPgType(f)
                            + ("REQUIRED".equalsIgnoreCase(f.getMode()) ? " NOT NULL" : ""))
                    .collect(Collectors.joining(", "));
            postgres.execute("CREATE TABLE " + Fn.pgQuote(schema) + "." + Fn.pgQuote(tableId) + " (" + columns + ")");

            Catalog.TableInfo info = catalog.findTable(schema, tableId);
            return Response.ok(describe(projectId, datasetId, tableId, info, null)).build();
        } catch (RuntimeException e) {
            return Fn.toError(e);
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
                return Fn.bigQueryError(Response.Status.NOT_FOUND,
                        "Not found: Table " + projectId + ":" + datasetId + "." + tableId);

            postgres.execute("DROP " + (info.view() ? "VIEW " : "TABLE ") + Fn.pgQuote(schema) + "." + Fn.pgQuote(info.name()));
            return Response.noContent().build();
        } catch (RuntimeException e) {
            return Fn.toError(e);
        }
    }

    private Table describe(String projectId, String datasetId, String tableId, Catalog.TableInfo info, String selectedFields) {
        String schema = Fn.toSchema(projectId, datasetId);
        Set<String> selected = isBlank(selectedFields)
                ? null
                : java.util.Arrays.stream(selectedFields.split(",")).map(s -> s.trim().toLowerCase()).collect(Collectors.toSet());

        List<TableFieldSchema> fields = catalog.columns(schema, info.name()).stream()
                .filter(c -> selected == null || selected.contains(c.name().toLowerCase()))
                .map(c -> {
                    BqTypes.BqType type = BqTypes.fromPg(c.pgType());
                    return new TableFieldSchema()
                            .name(c.name())
                            .type(type.type())
                            .mode(type.repeated() ? "REPEATED" : c.nullable() ? "NULLABLE" : "REQUIRED");
                })
                .toList();

        long numRows = postgres.count("SELECT COUNT(*) FROM " + Fn.pgQuote(schema) + "." + Fn.pgQuote(info.name()));
        String now = String.valueOf(System.currentTimeMillis());

        // Storage statistics are not tracked; only the row count is real.
        Table table = new Table(now, BQType.etag(projectId, datasetId, tableId),
                projectId + ":" + datasetId + "." + tableId, now, "US",
                "0", "0", "0", null, null, null, null, "0", String.valueOf(numRows), null, null, null, null,
                "/bigquery/v2/projects/" + projectId + "/datasets/" + datasetId + "/tables/" + tableId,
                info.view() ? "VIEW" : "TABLE");
        table.setKind("bigquery#table");
        table.setFriendlyName(tableId);
        table.setTableReference(BQType.tableReference(projectId, datasetId, tableId));
        table.setSchema(new TableSchema().fields(fields));
        return table;
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
