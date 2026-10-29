package dev.meena.bigquery.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.*;
import dev.meena.test.StartPostgresContainers;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Creates a table with every BigQuery column type in two datasets (one by DDL, one by the tables API),
 * streams rows into both and reads every column back through the BigQuery SDK.
 * <p>
 * The tables are never dropped at the end, so they can be inspected afterwards:
 * {@code "typetest$types_ddl".all_types} and {@code "typetest$types_api".all_types}. They are replaced
 * at the start of the next run. To run against an existing Postgres instead of a container, pass
 * {@code -Dtest.postgres.url=... -Dtest.postgres.user=... -Dtest.postgres.password=...}.
 */
@QuarkusTest
@QuarkusTestResource(StartPostgresContainers.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestAllTypesTables {

    private static final String PROJECT = "typetest";
    private static final String DDL_DATASET = "types_ddl";
    private static final String API_DATASET = "types_api";
    private static final String TABLE = "all_types";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> TYPE_COLUMNS = List.of(
            "c_int", "c_float", "c_numeric", "c_bignumeric", "c_bool", "c_string", "c_bytes", "c_date", "c_time",
            "c_datetime", "c_timestamp", "c_geography", "c_json", "c_interval", "c_range", "c_int_array",
            "c_string_array", "c_timestamp_array", "c_struct", "c_struct_array");

    private static BigQuery bq;

    @BeforeAll
    static void setUp() throws InterruptedException {
        bq = BigQueryOptions.newBuilder()
                .setProjectId(PROJECT)
                .setHost("http://localhost:" + ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class))
                .setLocation("US")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();

        for (String dataset : List.of(DDL_DATASET, API_DATASET)) {
            if (bq.getDataset(DatasetId.of(PROJECT, dataset)) == null)
                bq.create(DatasetInfo.of(DatasetId.of(PROJECT, dataset)));
        }

        // Replace tables of an earlier run (they are left in place after this one)
        bq.query(QueryJobConfiguration.of("DROP TABLE IF EXISTS `" + DDL_DATASET + "`.`" + TABLE + "`"));
        bq.delete(TableId.of(PROJECT, API_DATASET, TABLE));
    }

    // ---- create

    @Test
    @Order(1)
    void createTableWithDdl() throws InterruptedException {
        bq.query(QueryJobConfiguration.of("""
                CREATE TABLE `types_ddl`.`all_types` (
                  c_int INT64 NOT NULL,
                  c_float FLOAT64,
                  c_numeric NUMERIC,
                  c_bignumeric BIGNUMERIC,
                  c_bool BOOL,
                  c_string STRING,
                  c_bytes BYTES,
                  c_date DATE,
                  c_time TIME,
                  c_datetime DATETIME,
                  c_timestamp TIMESTAMP,
                  c_geography GEOGRAPHY,
                  c_json JSON,
                  c_interval INTERVAL,
                  c_range RANGE<DATE>,
                  c_int_array ARRAY<INT64>,
                  c_string_array ARRAY<STRING>,
                  c_timestamp_array ARRAY<TIMESTAMP>,
                  c_struct STRUCT<a INT64, b STRING>,
                  c_struct_array ARRAY<STRUCT<a INT64, b STRING>>
                )
                OPTIONS(description = 'every BigQuery type')
                """));

        assertNotNull(bq.getTable(TableId.of(PROJECT, DDL_DATASET, TABLE)));
    }

    @Test
    @Order(2)
    void createTableWithTablesApi() {
        Schema schema = Schema.of(
                Field.newBuilder("c_int", StandardSQLTypeName.INT64).setMode(Field.Mode.REQUIRED).build(),
                Field.of("c_float", StandardSQLTypeName.FLOAT64),
                Field.of("c_numeric", StandardSQLTypeName.NUMERIC),
                Field.of("c_bignumeric", StandardSQLTypeName.BIGNUMERIC),
                Field.of("c_bool", StandardSQLTypeName.BOOL),
                Field.of("c_string", StandardSQLTypeName.STRING),
                Field.of("c_bytes", StandardSQLTypeName.BYTES),
                Field.of("c_date", StandardSQLTypeName.DATE),
                Field.of("c_time", StandardSQLTypeName.TIME),
                Field.of("c_datetime", StandardSQLTypeName.DATETIME),
                Field.of("c_timestamp", StandardSQLTypeName.TIMESTAMP),
                Field.of("c_geography", StandardSQLTypeName.GEOGRAPHY),
                Field.of("c_json", StandardSQLTypeName.JSON),
                Field.of("c_interval", StandardSQLTypeName.INTERVAL),
                Field.newBuilder("c_range", StandardSQLTypeName.RANGE)
                        .setRangeElementType(FieldElementType.newBuilder().setType("DATE").build()).build(),
                Field.newBuilder("c_int_array", StandardSQLTypeName.INT64).setMode(Field.Mode.REPEATED).build(),
                Field.newBuilder("c_string_array", StandardSQLTypeName.STRING).setMode(Field.Mode.REPEATED).build(),
                Field.newBuilder("c_timestamp_array", StandardSQLTypeName.TIMESTAMP).setMode(Field.Mode.REPEATED).build(),
                Field.of("c_struct", StandardSQLTypeName.STRUCT,
                        Field.of("a", StandardSQLTypeName.INT64), Field.of("b", StandardSQLTypeName.STRING)),
                Field.newBuilder("c_struct_array", StandardSQLTypeName.STRUCT,
                        Field.of("a", StandardSQLTypeName.INT64), Field.of("b", StandardSQLTypeName.STRING))
                        .setMode(Field.Mode.REPEATED).build());

        bq.create(TableInfo.of(TableId.of(PROJECT, API_DATASET, TABLE), StandardTableDefinition.of(schema)));

        assertNotNull(bq.getTable(TableId.of(PROJECT, API_DATASET, TABLE)));
    }

    @Test
    @Order(3)
    void tablesLiveInDifferentDatasets() {
        for (String dataset : List.of(DDL_DATASET, API_DATASET)) {
            List<String> names = new ArrayList<>();
            bq.listTables(DatasetId.of(PROJECT, dataset)).iterateAll().forEach(t -> names.add(t.getTableId().getTable()));
            assertEquals(List.of(TABLE), names, "tables in " + dataset);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {DDL_DATASET, API_DATASET})
    @Order(4)
    void schemaReportsBigQueryTypes(String dataset) {
        FieldList fields = bq.getTable(TableId.of(PROJECT, dataset, TABLE)).getDefinition().getSchema().getFields();

        assertEquals(TYPE_COLUMNS, fields.stream().map(Field::getName).toList());
        assertType(fields, "c_int", StandardSQLTypeName.INT64, Field.Mode.REQUIRED);
        assertType(fields, "c_float", StandardSQLTypeName.FLOAT64, Field.Mode.NULLABLE);
        assertType(fields, "c_numeric", StandardSQLTypeName.NUMERIC, Field.Mode.NULLABLE);
        assertType(fields, "c_bignumeric", StandardSQLTypeName.NUMERIC, Field.Mode.NULLABLE); // both are Postgres numeric
        assertType(fields, "c_bool", StandardSQLTypeName.BOOL, Field.Mode.NULLABLE);
        assertType(fields, "c_string", StandardSQLTypeName.STRING, Field.Mode.NULLABLE);
        assertType(fields, "c_bytes", StandardSQLTypeName.BYTES, Field.Mode.NULLABLE);
        assertType(fields, "c_date", StandardSQLTypeName.DATE, Field.Mode.NULLABLE);
        assertType(fields, "c_time", StandardSQLTypeName.TIME, Field.Mode.NULLABLE);
        assertType(fields, "c_datetime", StandardSQLTypeName.DATETIME, Field.Mode.NULLABLE);
        assertType(fields, "c_timestamp", StandardSQLTypeName.TIMESTAMP, Field.Mode.NULLABLE);
        assertType(fields, "c_geography", StandardSQLTypeName.STRING, Field.Mode.NULLABLE); // stored as WKT text
        assertType(fields, "c_json", StandardSQLTypeName.JSON, Field.Mode.NULLABLE);
        assertType(fields, "c_interval", StandardSQLTypeName.INTERVAL, Field.Mode.NULLABLE);
        assertType(fields, "c_range", StandardSQLTypeName.STRING, Field.Mode.NULLABLE); // Postgres range types are not mapped back
        assertType(fields, "c_int_array", StandardSQLTypeName.INT64, Field.Mode.REPEATED);
        assertType(fields, "c_string_array", StandardSQLTypeName.STRING, Field.Mode.REPEATED);
        assertType(fields, "c_timestamp_array", StandardSQLTypeName.TIMESTAMP, Field.Mode.REPEATED);
        assertType(fields, "c_struct", StandardSQLTypeName.JSON, Field.Mode.NULLABLE); // stored as JSONB
        assertType(fields, "c_struct_array", StandardSQLTypeName.JSON, Field.Mode.REPEATED);
    }

    private static void assertType(FieldList fields, String name, StandardSQLTypeName type, Field.Mode mode) {
        Field field = fields.get(name);
        assertEquals(type, field.getType().getStandardType(), name + " type");
        assertEquals(mode, field.getMode() == null ? Field.Mode.NULLABLE : field.getMode(), name + " mode");
    }

    // ---- insert and read back

    private static Map<String, Object> fullRow(long id) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("c_int", id);
        row.put("c_float", 1.5);
        row.put("c_numeric", "12345678901234567890.123456789");
        row.put("c_bignumeric", "578960446186580977117854925043439539266.34992332820282019728792003956564819967");
        row.put("c_bool", true);
        row.put("c_string", "héllo 'world' \"quoted\" \\ backslash");
        row.put("c_bytes", "AAEC/w==");
        row.put("c_date", "2020-02-29");
        row.put("c_time", "12:34:56.123456");
        row.put("c_datetime", "2020-02-29T12:34:56.123456");
        row.put("c_timestamp", "2020-02-29 12:34:56.123456 UTC");
        row.put("c_geography", "POINT(-122.35 37.55)");
        row.put("c_json", "{\"a\": [1, 2, {\"b\": null}], \"c\": \"d\"}");
        row.put("c_interval", "1-2 3 4:5:6.5");
        row.put("c_range", "[2020-01-01, 2020-12-31)");
        row.put("c_int_array", List.of(1, 2, 3));
        row.put("c_string_array", List.of("a", "b,c", "d\"e"));
        row.put("c_timestamp_array", List.of("2020-01-01 00:00:00 UTC", "2021-06-01T10:00:00+02:00"));
        row.put("c_struct", Map.of("a", 7, "b", "seven"));
        row.put("c_struct_array", List.of(Map.of("a", 1, "b", "one"), Map.of("a", 2, "b", "two")));
        return row;
    }

    private static Map<String, Object> nullRow(long id) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("c_int", id);
        return row;
    }

    @ParameterizedTest
    @ValueSource(strings = {DDL_DATASET, API_DATASET})
    @Order(5)
    void insertRowsOfEveryType(String dataset) {
        InsertAllResponse response = bq.insertAll(InsertAllRequest.newBuilder(TableId.of(PROJECT, dataset, TABLE))
                .addRow(fullRow(1))
                .addRow(nullRow(2))
                .build());

        assertFalse(response.hasErrors(), () -> "insert errors: " + response.getInsertErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {DDL_DATASET, API_DATASET})
    @Order(6)
    void readEveryTypeBack(String dataset) throws Exception {
        TableResult result = bq.query(QueryJobConfiguration.of("SELECT * FROM `" + dataset + "`.`" + TABLE + "` ORDER BY c_int"));
        List<FieldValueList> rows = new ArrayList<>();
        result.iterateAll().forEach(rows::add);
        assertEquals(2, rows.size());

        FieldValueList row = rows.get(0);
        assertEquals(1L, row.get("c_int").getLongValue());
        assertEquals(1.5, row.get("c_float").getDoubleValue());
        assertEquals(new BigDecimal("12345678901234567890.123456789"), row.get("c_numeric").getNumericValue());
        assertEquals("578960446186580977117854925043439539266.34992332820282019728792003956564819967",
                row.get("c_bignumeric").getStringValue());
        assertTrue(row.get("c_bool").getBooleanValue());
        assertEquals("héllo 'world' \"quoted\" \\ backslash", row.get("c_string").getStringValue());
        assertArrayEquals(new byte[]{0, 1, 2, (byte) 255}, row.get("c_bytes").getBytesValue());
        assertEquals("2020-02-29", row.get("c_date").getStringValue());
        assertEquals("12:34:56.123456", row.get("c_time").getStringValue());
        assertEquals("2020-02-29T12:34:56.123456", row.get("c_datetime").getStringValue());
        assertEquals(micros("2020-02-29T12:34:56.123456Z"), row.get("c_timestamp").getTimestampValue());
        assertEquals("POINT(-122.35 37.55)", row.get("c_geography").getStringValue());
        assertEquals(MAPPER.readTree("{\"a\": [1, 2, {\"b\": null}], \"c\": \"d\"}"), MAPPER.readTree(row.get("c_json").getStringValue()));
        assertEquals("1-2 3 4:5:6.5", row.get("c_interval").getStringValue());
        assertEquals("[2020-01-01,2020-12-31)", row.get("c_range").getStringValue());

        assertEquals(List.of(1L, 2L, 3L), row.get("c_int_array").getRepeatedValue().stream().map(FieldValue::getLongValue).toList());
        assertEquals(List.of("a", "b,c", "d\"e"), row.get("c_string_array").getRepeatedValue().stream().map(FieldValue::getStringValue).toList());
        assertEquals(List.of(micros("2020-01-01T00:00:00Z"), micros("2021-06-01T08:00:00Z")),
                row.get("c_timestamp_array").getRepeatedValue().stream().map(FieldValue::getTimestampValue).toList());
        assertEquals(MAPPER.readTree("{\"a\": 7, \"b\": \"seven\"}"), MAPPER.readTree(row.get("c_struct").getStringValue()));
        assertEquals(MAPPER.readTree("[{\"a\": 1, \"b\": \"one\"}, {\"a\": 2, \"b\": \"two\"}]"),
                MAPPER.readTree("[" + String.join(",", row.get("c_struct_array").getRepeatedValue().stream().map(FieldValue::getStringValue).toList()) + "]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {DDL_DATASET, API_DATASET})
    @Order(7)
    void nullsReadBackAsNulls(String dataset) throws InterruptedException {
        TableResult result = bq.query(QueryJobConfiguration.of("SELECT * FROM `" + dataset + "`.`" + TABLE + "` WHERE c_int = 2"));
        FieldValueList row = result.iterateAll().iterator().next();

        for (String column : TYPE_COLUMNS) {
            if (column.equals("c_int")) continue;
            FieldValue value = row.get(column);
            if (value.getAttribute() == FieldValue.Attribute.REPEATED)
                assertTrue(value.isNull() || value.getRepeatedValue().isEmpty(), column);
            else
                assertTrue(value.isNull(), column + " should be null");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {DDL_DATASET, API_DATASET})
    @Order(8)
    void tableDataEndpointReturnsTheSameValues(String dataset) {
        var rows = bq.listTableData(TableId.of(PROJECT, dataset, TABLE), bq.getTable(TableId.of(PROJECT, dataset, TABLE)).getDefinition().getSchema());
        FieldValueList row = rows.iterateAll().iterator().next();

        assertEquals("2020-02-29", row.get("c_date").getStringValue());
        assertEquals(micros("2020-02-29T12:34:56.123456Z"), row.get("c_timestamp").getTimestampValue());
        assertEquals(3, row.get("c_int_array").getRepeatedValue().size());
    }

    @Test
    @Order(9)
    void insertWithWrongTypeReportsInsertError() {
        InsertAllResponse response = bq.insertAll(InsertAllRequest.newBuilder(TableId.of(PROJECT, DDL_DATASET, TABLE))
                .addRow(Map.of("c_int", "not a number"))
                .build());

        assertTrue(response.hasErrors());
    }

    /** Microseconds since the epoch, which is how the SDK reports timestamps. */
    private static long micros(String instant) {
        var i = java.time.Instant.parse(instant);
        return i.getEpochSecond() * 1_000_000 + i.getNano() / 1_000;
    }
}
