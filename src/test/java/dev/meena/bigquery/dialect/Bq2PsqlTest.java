package dev.meena.bigquery.dialect;

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
        assertEquals("SELECT id, value\nFROM public.table", translated);
    }

    @Test
    void translateLike() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT `id`, `value` FROM `public`.`table` WHERE `id` LIKE 'test%'");
        assertEquals("SELECT id, value\nFROM public.table\nWHERE id LIKE 'test%'", translated);
    }

    @Test
    void translateTableParts() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT `id`, `value` FROM `projectID`.`public`.`table` WHERE `id` = 123", "defaultProj");
        assertEquals("SELECT id, value\nFROM \"projectid$public\".table\nWHERE id = 123", translated);
    }

    @Test
    void noTableQuotesOnUnderscore() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT id, val FROM project.public.`table_1` WHERE id = 123", "projectID2");
        assertEquals("SELECT id, val\nFROM \"project$public\".table_1\nWHERE id = 123", translated);
    }

    @Test
    void insertAll() {
        Bq2Psql bq = new Bq2Psql();

        TableDataInsertAllRequest request = new TableDataInsertAllRequest();
        request.setRows(List.of(row(1, "test1"), row(2, "test2"), row(3, "test3")));

        Bq2Psql.InsertAll bqInserts = bq.insertAll(request, "test", "public", "integration_test");

        assertEquals("INSERT INTO \"test$public\".\"integration_test\" (\"num\", \"text\") VALUES (?, ?) ON CONFLICT DO NOTHING;", bqInserts.preparedSql());
        assertArrayEquals(new Object[]{1, "test1"}, bqInserts.rows().get(0));
        assertArrayEquals(new Object[]{2, "test2"}, bqInserts.rows().get(1));
        assertArrayEquals(new Object[]{3, "test3"}, bqInserts.rows().get(2));
    }

    private static TableDataInsertAllRequestRowsInner row(int num, String value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("num", num);
        row.put("text", value);

        TableDataInsertAllRequestRowsInner rowContainer = new TableDataInsertAllRequestRowsInner();
        rowContainer.setInsertId("id");
        rowContainer.setJson(row);
        return rowContainer;
    }
}