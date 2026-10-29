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
        assertEquals("SELECT id, value\nFROM public.\"table\"", translated);
    }

    @Test
    void translateLike() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT `id`, `value` FROM `public`.`table` WHERE `id` LIKE 'test%'");
        assertEquals("SELECT id, value\nFROM public.\"table\"\nWHERE id LIKE 'test%'", translated);
    }

    @Test
    void translateTableParts() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT `id`, `value` FROM `projectID`.`public`.`table` WHERE `id` = 123", "defaultProj");
        assertEquals("SELECT id, value\nFROM \"projectid$public\".\"table\"\nWHERE id = 123", translated);
    }

    @Test
    void noTableQuotesOnUnderscore() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT id, val FROM project.public.`table_1` WHERE id = 123", "projectID2");
        assertEquals("SELECT id, val\nFROM \"project$public\".table_1\nWHERE id = 123", translated);
    }

    @Test
    void translateDropTable() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("DROP TABLE IF EXISTS `projectID`.`public`.`table`");
        assertEquals("DROP TABLE IF EXISTS \"projectid$public\".\"table\"", translated);
    }

    @Test
    void translateDropTableWithDefaultProject() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("DROP TABLE IF EXISTS `public`.`table`", "projectID2");
        assertEquals("DROP TABLE IF EXISTS \"projectid2$public\".\"table\"", translated);
    }

    @Test
    void translateCreateTable() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("CREATE TABLE IF NOT EXISTS `projectID`.`public`.`table` AS SELECT 1");
        assertEquals("CREATE TABLE IF NOT EXISTS \"projectid$public\".\"table\" AS SELECT 1", translated);
    }

    @Test
    void translateCreateTableWithDefaultProject() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("CREATE OR REPLACE TABLE `public`.`table` AS SELECT 1", "projectID2");
        assertEquals("DROP TABLE IF EXISTS \"projectid2$public\".\"table\"; CREATE TABLE \"projectid2$public\".\"table\" AS SELECT 1", translated);
    }

    @Test
    void translateInformationSchemaWithDefaultProject() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String query = """
                SELECT table_name, table_type
                FROM information_schema.tables
                WHERE table_schema = 'local$udp'
                  AND table_name = 'DIM_LOCATION'
                  AND table_type IN ('BASE TABLE', 'VIEW')
                LIMIT 1
                """;
        String translated = bq.translate(query, "local");
        assertEquals("""
                SELECT table_name, table_type
                FROM information_schema.tables
                WHERE LOWER(table_schema) = LOWER('local$udp') AND LOWER(table_name) = LOWER('DIM_LOCATION') AND table_type IN ('BASE TABLE', 'VIEW')
                FETCH NEXT 1 ROWS ONLY""", translated);
    }

    @Test
    void translateDatasetQualifiedInformationSchema() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("SELECT table_name FROM `local`.`udp`.INFORMATION_SCHEMA.TABLES", "local");
        assertEquals("SELECT table_name\nFROM information_schema.tables", translated);
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

    @Test
    void createTableTranslatesEveryBigQueryType() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        String translated = bq.translate("""
                CREATE OR REPLACE TABLE `proj`.`ds`.`all_types` (
                  c_int INT64 NOT NULL,
                  c_float FLOAT64,
                  c_numeric NUMERIC(10, 2),
                  c_bignumeric BIGNUMERIC,
                  c_bool BOOL,
                  c_string STRING(10) OPTIONS(description = 'a, b (c)'),
                  c_bytes BYTES,
                  c_date DATE,
                  c_time TIME,
                  c_datetime DATETIME,
                  c_timestamp TIMESTAMP,
                  c_geo GEOGRAPHY,
                  c_json JSON,
                  c_interval INTERVAL,
                  c_range RANGE<DATE>,
                  c_array ARRAY<INT64>,
                  c_struct STRUCT<a INT64, b ARRAY<STRING>>,
                  c_struct_array ARRAY<STRUCT<a INT64, b STRING>>
                )
                PARTITION BY DATE(c_timestamp)
                CLUSTER BY c_int
                OPTIONS(description = 'x')
                """);

        assertEquals("DROP TABLE IF EXISTS \"proj$ds\".all_types; CREATE TABLE \"proj$ds\".all_types ("
                + "\"c_int\" BIGINT NOT NULL, \"c_float\" DOUBLE PRECISION, \"c_numeric\" NUMERIC(10,2), \"c_bignumeric\" NUMERIC, "
                + "\"c_bool\" BOOLEAN, \"c_string\" TEXT, \"c_bytes\" BYTEA, \"c_date\" DATE, \"c_time\" TIME, "
                + "\"c_datetime\" TIMESTAMP, \"c_timestamp\" TIMESTAMPTZ, \"c_geo\" TEXT, \"c_json\" JSONB, \"c_interval\" INTERVAL, "
                + "\"c_range\" DATERANGE, \"c_array\" BIGINT[], \"c_struct\" JSONB, \"c_struct_array\" JSONB[])", translated);
    }

    @Test
    void createTableWithoutSpaceBeforeColumnsAndLowercasesColumnNames() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        assertEquals("CREATE TABLE IF NOT EXISTS \"p$d\".t (\"mixedcase\" TEXT, \"n\" BIGINT)",
                bq.translate("CREATE TABLE IF NOT EXISTS d.t(MixedCase STRING, `n` INT64)", "p"));
    }

    @Test
    void createTableAsSelectTranslatesTheQuery() throws SqlParseException {
        Bq2Psql bq = new Bq2Psql();

        assertEquals("CREATE TABLE \"p$d\".copy AS SELECT *\nFROM \"p$d\".src",
                bq.translate("CREATE TABLE d.copy OPTIONS(description='x') AS SELECT * FROM `d`.`src`", "p"));
    }

    @Test
    void insertAllMatchesColumnsCaseInsensitively() {
        Bq2Psql bq = new Bq2Psql();
        TableDataInsertAllRequestRowsInner row = new TableDataInsertAllRequestRowsInner();
        row.setJson(new LinkedHashMap<>(Map.of("MixedCase", "v")));
        TableDataInsertAllRequest request = new TableDataInsertAllRequest();
        request.setRows(List.of(row));

        Bq2Psql.InsertAll insertAll = bq.insertAll(request, "p", "d", "t");
        assertEquals(List.of("mixedcase"), insertAll.columns());
        assertArrayEquals(new Object[]{"v"}, insertAll.rows().getFirst());
    }
}
