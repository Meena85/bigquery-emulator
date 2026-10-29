package dev.meena.bigquery.fn;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.BqTypes;
import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FnTest {

    private static Fn.ErrorResponse body(Response response) {
        return (Fn.ErrorResponse) response.getEntity();
    }

    @Test
    void errorEnvelopeMatchesBigQueryShape() {
        Response r = Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Table p:d.t");
        assertEquals(404, r.getStatus());
        assertEquals(404, body(r).error().code());
        assertEquals("NOT_FOUND", body(r).error().status());
        assertEquals("notFound", body(r).error().errors().getFirst().reason());
        assertEquals("global", body(r).error().errors().getFirst().domain());
    }

    @Test
    void toErrorMapsExceptions() {
        SqlParseException parse = new SqlParseException("bad sql", SqlParserPos.ZERO, null, null, null);
        assertEquals(400, Fn.toError(parse).getStatus());
        assertEquals("invalidQuery", body(Fn.toError(parse)).error().errors().getFirst().reason());
        assertEquals(400, Fn.toError(new IllegalArgumentException("x")).getStatus());
        assertEquals(404, Fn.toError(new IllegalStateException("Not found: Table a")).getStatus());
        assertEquals(409, Fn.toError(new IllegalStateException("Already Exists: Table a")).getStatus());
        assertEquals(500, Fn.toError(new IllegalStateException("boom")).getStatus());
    }

    @Test
    void queryResponseHasSchemaRowsAndKind() {
        var result = new Postgres.QueryResult(
                List.of(new Postgres.Column("n", Types.BIGINT, "int8"), new Postgres.Column("s", Types.VARCHAR, "text")),
                List.of(Arrays.asList(1L, "a"), Arrays.asList(2L, null)));

        var response = Fn.queryResponse("p", "job1", "US", result);
        assertEquals("bigquery#queryResponse", response.getKind());
        assertTrue(response.getJobComplete());
        assertEquals("2", response.getTotalRows());
        assertEquals("job1", response.getJobReference().getJobId());
        assertEquals("INTEGER", response.getSchema().getFields().getFirst().getType());
        assertEquals("1", response.getRows().getFirst().getF().getFirst().getV());
        assertNull(response.getRows().get(1).getF().get(1).getV());
    }

    @Test
    void toSchemaLowercasesAndJoins() {
        assertEquals("proj$ds", Fn.toSchema("Proj", "DS"));
        assertEquals("information_schema", Fn.toSchema("p", "information_schema"));
    }

    private static Object encode(Object value, String type) {
        return Fn.toBigQueryValue(value, new BqTypes.BqType(type, false));
    }

    @Test
    void encodesScalarTypesLikeTheRestApi() {
        assertNull(encode(null, "STRING"));
        assertEquals("42", encode(42L, "INTEGER"));
        assertEquals("1.5", encode(1.5, "FLOAT"));
        assertEquals("NaN", encode(Double.NaN, "FLOAT"));
        assertEquals("1000000000000000000000", encode(new java.math.BigDecimal("1E+21"), "NUMERIC"));
        assertEquals("true", encode(true, "BOOLEAN"));
        assertEquals("text", encode("text", "STRING"));
        assertEquals("AAEC/w==", encode(new byte[]{0, 1, 2, (byte) 255}, "BYTES"));
        assertEquals("2020-01-02", encode(java.time.LocalDate.of(2020, 1, 2), "DATE"));
        assertEquals("2020-01-02", encode(java.sql.Date.valueOf("2020-01-02"), "DATE"));
        assertEquals("12:34:56.123456", encode(java.time.LocalTime.of(12, 34, 56, 123_456_000), "TIME"));
        assertEquals("01:00:00", encode(java.time.LocalTime.of(1, 0), "TIME"));
        assertEquals("2020-01-02T03:04:05", encode(java.time.LocalDateTime.of(2020, 1, 2, 3, 4, 5), "DATETIME"));
        assertEquals("{\"a\": 1}", encode("{\"a\": 1}", "JSON"));
    }

    @Test
    void intervalUsesBigQueryCanonicalForm() throws java.sql.SQLException {
        assertEquals("1-2 3 4:5:6.5", encode(new org.postgresql.util.PGInterval("1 year 2 mons 3 days 4:05:06.5"), "INTERVAL"));
        assertEquals("-1-2 3 -4:5:6", encode(new org.postgresql.util.PGInterval("-1 years -2 mons 3 days -4:05:06"), "INTERVAL"));
        assertEquals("0-0 0 0:0:0", encode(new org.postgresql.util.PGInterval("0 seconds"), "INTERVAL"));
    }

    @Test
    void timestampIsEpochSecondsWithMicros() {
        assertEquals("1577836800", encode(java.time.OffsetDateTime.parse("2020-01-01T00:00:00Z"), "TIMESTAMP"));
        assertEquals("1577836800.123456", encode(java.time.OffsetDateTime.parse("2020-01-01T01:00:00.123456+01:00"), "TIMESTAMP"));
        assertEquals("-1.5", encode(java.time.OffsetDateTime.parse("1969-12-31T23:59:58.5Z"), "TIMESTAMP"));
    }

    @Test
    void repeatedValuesBecomeListsOfCells() {
        @SuppressWarnings("unchecked")
        var cells = (List<dev.meena.bigquery.model.TableCell>) Fn.toBigQueryValue(new Long[]{1L, null, 3L}, new BqTypes.BqType("INTEGER", true));
        assertEquals(3, cells.size());
        assertEquals("1", cells.get(0).getV());
        assertNull(cells.get(1).getV());
        assertEquals("3", cells.get(2).getV());
    }

    @Test
    void schemaReportsRepeatedAndTemporalTypes() {
        var schema = Fn.tableSchema(List.of(
                new Postgres.Column("a", Types.ARRAY, "_text"),
                new Postgres.Column("b", Types.TIMESTAMP, "timestamptz"),
                new Postgres.Column("c", Types.TIMESTAMP, "timestamp"),
                new Postgres.Column("d", Types.BINARY, "bytea")));
        assertEquals("REPEATED", schema.getFields().get(0).getMode());
        assertEquals("STRING", schema.getFields().get(0).getType());
        assertEquals("TIMESTAMP", schema.getFields().get(1).getType());
        assertEquals("DATETIME", schema.getFields().get(2).getType());
        assertEquals("BYTES", schema.getFields().get(3).getType());
    }
}
