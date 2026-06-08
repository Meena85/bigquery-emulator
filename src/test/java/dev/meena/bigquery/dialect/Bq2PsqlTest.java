package dev.meena.bigquery.dialect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.TableDataInsertAllRequest;
import dev.meena.bigquery.model.TableDataInsertAllRequestRowsInner;
import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Bq2PsqlTest {

    @Test
    void translateSelect() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT `id`, `value` FROM `public`.`table`");
        assertEquals("SELECT \"id\", \"value\"\nFROM \"public\".\"table\"", translated);
    }

    @Test
    void translateLike() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT `id`, `value` FROM `public`.`table` WHERE `id` LIKE 'test%'");
        assertEquals("SELECT \"id\", \"value\"\nFROM \"public\".\"table\"\nWHERE \"id\" LIKE 'test%'", translated);
    }

    @Test
    void insertAll() {
        Bq2Psql bq = new Bq2Psql();

        TableDataInsertAllRequest request = new TableDataInsertAllRequest();
        request.setRows(List.of( row(1, "test1"), row(2, "test2"), row(3, "test3") ));

        Bq2Psql.InsertAll bqInserts = bq.insertAll(request, "public", "integration_test");

        assertEquals("INSERT INTO \"public\".\"integration_test\" (\"num\", \"value\") VALUES (?, ?) ON CONFLICT DO NOTHING;", bqInserts.preparedSql());
        assertArrayEquals(new Object[]{ 1, "test1" }, bqInserts.rows().get(0));
        assertArrayEquals(new Object[]{ 2, "test2" }, bqInserts.rows().get(1));
        assertArrayEquals(new Object[]{ 3, "test3" }, bqInserts.rows().get(2));
    }

    @Test
    void toResponse() throws JsonProcessingException {
        List<Postgres.Column> columns = List.of(
                new Postgres.Column("num", 1, "string"),
                new Postgres.Column("value", 2, "unknown"));
        List<List<Object>> rows = List.of(List.of(1, "test1"), List.of(2, "test2"), List.of(3, "test3"));
        var r = new Postgres.QueryResult(columns, rows);

        var r1 = Fn.toBigQueryQueryResponse("project_id", "job_id", "US_LOC", r);
        var r2 = Fn.toBigQueryQueryResponse2("project_id", "job_id", "US_LOC", r);

        ObjectMapper m = new ObjectMapper();
        String s1 = m.writeValueAsString(r1);
        String s2 = m.writeValueAsString(r2);

        assertEquals(s1, s2);
    }

    private static TableDataInsertAllRequestRowsInner row(int num, String value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("num", num);
        row.put("value", value);

        TableDataInsertAllRequestRowsInner rowContainer = new TableDataInsertAllRequestRowsInner();
        rowContainer.setInsertId("id");
        rowContainer.setJson(row);
        return rowContainer;
    }
}