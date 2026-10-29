package dev.meena.bigquery.responses;

import dev.meena.bigquery.dialect.BqTypes;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellValuesTest {

    private static Object encode(Object value, String type) {
        return CellValues.encode(value, new BqTypes.BqType(type, false));
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
        var cells = (List<dev.meena.bigquery.model.TableCell>) CellValues.encode(new Long[]{1L, null, 3L}, new BqTypes.BqType("INTEGER", true));
        assertEquals(3, cells.size());
        assertEquals("1", cells.get(0).getV());
        assertNull(cells.get(1).getV());
        assertEquals("3", cells.get(2).getV());
    }
}
