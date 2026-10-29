package dev.meena.bigquery.responses;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.BqTypes;
import dev.meena.bigquery.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds query and job result payloads (schema, rows, job reference) from Postgres results.
 */
public final class QueryResponses {

    private QueryResponses() {
    }

    public static GetQueryResultsResponse queryResultsResponse(String projectId, String jobId, String location,
                                                               Postgres.QueryResult result) {
        var r = new GetQueryResultsResponse(null, null);
        r.setEtag("\"" + jobId + "\"");
        r.setJobComplete(true);
        r.setCacheHit(false);
        r.setTotalRows(String.valueOf(result.rows().size()));
        r.setJobReference(jobReference(projectId, jobId, location));
        r.setSchema(tableSchema(result.columns()));
        r.setRows(tableRows(result.columns(), result.rows()));
        return r;
    }

    /**
     * Response of the synchronous {@code POST /queries} call.
     */
    public static QueryResponse queryResponse(String projectId, String jobId, String location,
                                              Postgres.QueryResult result) {
        var r = new QueryResponse(null, null);
        r.setJobComplete(true);
        r.setCacheHit(false);
        r.setTotalRows(String.valueOf(result.rows().size()));
        r.setJobReference(jobReference(projectId, jobId, location));
        r.setSchema(tableSchema(result.columns()));
        r.setRows(tableRows(result.columns(), result.rows()));
        return r;
    }

    public static JobReference jobReference(String projectId, String jobId, String location) {
        var jobRef = new JobReference();
        jobRef.setProjectId(projectId);
        jobRef.setJobId(jobId);
        jobRef.setLocation(location);
        return jobRef;
    }

    public static TableSchema tableSchema(List<Postgres.Column> columns) {
        var schema = new TableSchema();
        schema.setFields(columns.stream().map(column -> {
            BqTypes.BqType type = BqTypes.fromPg(column.databaseTypeName());
            return new TableFieldSchema()
                    .name(column.name())
                    .type(type.type())
                    .mode(type.repeated() ? "REPEATED" : "NULLABLE");
        }).toList());
        return schema;
    }

    public static List<TableRow> tableRows(List<Postgres.Column> columns, List<List<Object>> rows) {
        List<BqTypes.BqType> types = columns.stream().map(c -> BqTypes.fromPg(c.databaseTypeName())).toList();
        return rows.stream()
                .map(source -> {
                    List<TableCell> cells = new ArrayList<>(source.size());
                    for (int i = 0; i < source.size(); i++)
                        cells.add(new TableCell().v(CellValues.encode(source.get(i), types.get(i))));
                    return new TableRow().f(cells);
                })
                .toList();
    }
}
