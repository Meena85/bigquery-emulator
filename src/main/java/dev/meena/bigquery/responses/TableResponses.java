package dev.meena.bigquery.responses;

import dev.meena.bigquery.db.Catalog;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.BqTypes;
import dev.meena.bigquery.model.*;

import java.util.List;
import java.util.Set;

/**
 * Builds table metadata and table data payloads.
 */
public final class TableResponses {

    private static final String LOCATION = "US";

    private TableResponses() {
    }

    public static String etag(String projectId, String datasetId, String tableId) {
        return "\"" + projectId + ":" + datasetId + "." + tableId + "\"";
    }

    public static TableReference tableReference(String projectId, String datasetId, String tableName) {
        return new TableReference().projectId(projectId).datasetId(datasetId).tableId(tableName);
    }

    /**
     * A page of rows ({@code tabledata.list}); callers set the table's total row count.
     */
    public static TableDataList tableDataList(Postgres.QueryResult result) {
        return new TableDataList()
                .totalRows(String.valueOf(result.rows().size()))
                .rows(QueryResponses.tableRows(result.columns(), result.rows()));
    }

    public static TableListTablesInner tableListItem(String projectId, String datasetId, Catalog.TableInfo info) {
        return new TableListTablesInner()
                .kind("bigquery#table")
                .id(projectId + ":" + datasetId + "." + info.name())
                .friendlyName(info.name())
                .tableReference(tableReference(projectId, datasetId, info.name()))
                .type(info.view() ? "VIEW" : "TABLE");
    }

    public static TableList tableList(String projectId, String datasetId, List<TableListTablesInner> tables, long total) {
        return new TableList()
                .kind("bigquery#tableList")
                .etag("\"" + projectId + ":" + datasetId + ":tables\"")
                .tables(tables)
                .totalItems((int) total);
    }

    /**
     * Full table metadata ({@code tables.get}). {@code selectedFields} (lower case) limits the schema when not null.
     * Storage statistics are not tracked; only the row count is real.
     */
    public static Table table(String projectId, String datasetId, String tableId, Catalog.TableInfo info,
                              List<Catalog.ColumnInfo> columns, Set<String> selectedFields, long numRows) {
        List<TableFieldSchema> fields = columns.stream()
                .filter(c -> selectedFields == null || selectedFields.contains(c.name().toLowerCase()))
                .map(c -> {
                    BqTypes.BqType type = BqTypes.fromPg(c.pgType());
                    return new TableFieldSchema()
                            .name(c.name())
                            .type(type.type())
                            .mode(type.repeated() ? "REPEATED" : c.nullable() ? "NULLABLE" : "REQUIRED");
                })
                .toList();

        String now = String.valueOf(System.currentTimeMillis());
        Table table = new Table(now, etag(projectId, datasetId, tableId),
                projectId + ":" + datasetId + "." + tableId, now, LOCATION,
                "0", "0", "0", null, null, null, null, "0", String.valueOf(numRows), null, null, null, null,
                "/bigquery/v2/projects/" + projectId + "/datasets/" + datasetId + "/tables/" + tableId,
                info.view() ? "VIEW" : "TABLE");
        table.setKind("bigquery#table");
        table.setFriendlyName(tableId);
        table.setTableReference(tableReference(projectId, datasetId, tableId));
        table.setSchema(new TableSchema().fields(fields));
        return table;
    }
}
