package dev.meena.bigquery.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** One test per BigQuery type: how a JSON value from insertAll is bound for the matching Postgres column. */
class PgValuesTest {

    private static String pg(Object bound) {
        return ((PGobject) bound).getValue();
    }

    @Test
    void int64() {
        assertEquals(42L, PgValues.toParameter(42, "int8"));
        assertEquals(9223372036854775807L, PgValues.toParameter("9223372036854775807", "int8"));
        assertEquals(-1L, PgValues.toParameter("-1", "int4"));
    }

    @Test
    void float64() {
        assertEquals(1.5, PgValues.toParameter("1.5", "float8"));
        assertEquals(2.0, PgValues.toParameter(2, "float8"));
        assertTrue(Double.isNaN((Double) PgValues.toParameter("NaN", "float8")));
        assertEquals(Double.POSITIVE_INFINITY, PgValues.toParameter("Infinity", "float8"));
        assertEquals(Double.NEGATIVE_INFINITY, PgValues.toParameter("-inf", "float8"));
    }

    @Test
    void numericKeepsPrecision() {
        assertEquals(new BigDecimal("12345678901234567890.123456789"),
                PgValues.toParameter("12345678901234567890.123456789", "numeric"));
        assertEquals(new BigDecimal("0.1"), PgValues.toParameter(0.1, "numeric"));
    }

    @Test
    void bigNumeric() {
        String huge = "578960446186580977117854925043439539266.34992332820282019728792003956564819967";
        assertEquals(new BigDecimal(huge), PgValues.toParameter(huge, "numeric"));
    }

    @Test
    void bool() {
        assertEquals(true, PgValues.toParameter(true, "bool"));
        assertEquals(true, PgValues.toParameter("true", "bool"));
        assertEquals(false, PgValues.toParameter("false", "bool"));
    }

    @Test
    void string() {
        assertEquals("héllo 'quoted'", PgValues.toParameter("héllo 'quoted'", "text"));
        assertEquals("12", PgValues.toParameter(12, "varchar"));
    }

    @Test
    void bytesAreBase64Decoded() {
        assertArrayEquals(new byte[]{0, 1, 2, (byte) 255}, (byte[]) PgValues.toParameter("AAEC/w==", "bytea"));
    }

    @Test
    void date() {
        assertEquals(LocalDate.of(2020, 1, 2), PgValues.toParameter("2020-01-02", "date"));
        assertEquals(LocalDate.of(2020, 1, 2), PgValues.toParameter("2020-1-2", "date"));
    }

    @Test
    void time() {
        assertEquals(LocalTime.of(12, 34, 56, 123_456_000), PgValues.toParameter("12:34:56.123456", "time"));
        assertEquals(LocalTime.of(1, 2, 3), PgValues.toParameter("01:02:03", "time"));
    }

    @Test
    void datetime() {
        assertEquals(LocalDateTime.of(2020, 1, 2, 3, 4, 5, 600_000_000), PgValues.toParameter("2020-01-02 03:04:05.6", "timestamp"));
        assertEquals(LocalDateTime.of(2020, 1, 2, 3, 4, 5), PgValues.toParameter("2020-01-02T03:04:05", "timestamp"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2020-01-01 00:00:00 UTC|2020-01-01T00:00Z",
            "2020-01-01T12:30:45Z|2020-01-01T12:30:45Z",
            "2020-01-01 12:30:45|2020-01-01T12:30:45Z",
            "2020-01-01 12:30:45+08|2020-01-01T12:30:45+08:00",
            "2020-01-01 12:30:45-0530|2020-01-01T12:30:45-05:30",
            "2020-01-01 12:30:45.123456+02:00|2020-01-01T12:30:45.123456+02:00",
            "2020-01-01|2020-01-01T00:00Z",
            "1577836800|2020-01-01T00:00Z",
            "1.5778368E9|2020-01-01T00:00Z",
            "1577836800.123456|2020-01-01T00:00:00.123456Z"
    })
    void timestamp(String input, String expected) {
        assertEquals(OffsetDateTime.parse(expected), PgValues.toParameter(input, "timestamptz"));
    }

    @Test
    void timestampFromJsonNumber() {
        assertEquals(OffsetDateTime.parse("2020-01-01T00:00:00Z"), PgValues.toParameter(1577836800, "timestamptz"));
        assertEquals(OffsetDateTime.parse("2020-01-01T00:00:00.5Z"), PgValues.toParameter(1577836800.5, "timestamptz"));
    }

    @Test
    void json() {
        assertEquals("{\"a\": 1}", pg(PgValues.toParameter("{\"a\": 1}", "jsonb")));
        assertEquals("{\"a\":[1,2]}", pg(PgValues.toParameter(Map.of("a", List.of(1, 2)), "jsonb")));
        assertEquals("jsonb", ((PGobject) PgValues.toParameter("[]", "jsonb")).getType());
    }

    @Test
    void structIsStoredAsJson() {
        assertEquals("{\"x\":1}", pg(PgValues.toParameter(Map.of("x", 1), "jsonb")));
    }

    @Test
    void geographyIsWellKnownText() {
        assertEquals("POINT(1 2)", PgValues.toParameter("POINT(1 2)", "text"));
    }

    @Test
    void interval() {
        assertEquals("1 years 2 mons 3 days 4:5:6.5", pg(PgValues.toParameter("1-2 3 4:5:6.5", "interval")));
        assertEquals("-1 years -2 mons 3 days -4:5:6", pg(PgValues.toParameter("-1-2 3 -4:5:6", "interval")));
        assertEquals("P1Y2M3DT4H5M6S", pg(PgValues.toParameter("P1Y2M3DT4H5M6S", "interval")));
    }

    @Test
    void range() {
        assertEquals("[2020-01-01,2020-12-31)", pg(PgValues.toParameter("[2020-01-01, 2020-12-31)", "daterange")));
        assertEquals("[2020-01-01,)", pg(PgValues.toParameter("[2020-01-01, UNBOUNDED)", "daterange")));
        assertEquals("(,2020-12-31)", pg(PgValues.toParameter("[NULL, 2020-12-31)".replace("[", "("), "daterange")));
    }

    @Test
    void arrayOfInt64() {
        PGobject array = (PGobject) PgValues.toParameter(List.of(1, 2, 3), "_int8");
        assertEquals("_int8", array.getType());
        assertEquals("{\"1\",\"2\",\"3\"}", array.getValue());
    }

    @Test
    void arrayOfStringEscapesQuotesAndBackslashes() {
        assertEquals("{\"a\\\"b\",\"c\\\\d\",NULL}", pg(PgValues.toParameter(java.util.Arrays.asList("a\"b", "c\\d", null), "_text")));
    }

    @Test
    void arrayOfBoolBytesTimestampAndJson() {
        assertEquals("{\"t\",\"f\"}", pg(PgValues.toParameter(List.of(true, false), "_bool")));
        assertEquals("{\"\\\\x0001\"}", pg(PgValues.toParameter(List.of("AAE="), "_bytea")));
        assertEquals("{\"2020-01-01T00:00Z\"}", pg(PgValues.toParameter(List.of("2020-01-01 00:00:00 UTC"), "_timestamptz")));
        assertEquals("{\"{\\\"a\\\":1}\"}", pg(PgValues.toParameter(List.of(Map.of("a", 1)), "_jsonb")));
    }

    @Test
    void emptyArray() {
        assertEquals("{}", pg(PgValues.toParameter(List.of(), "_int8")));
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(RuntimeException.class, () -> PgValues.toParameter("abc", "int8"));
        assertThrows(IllegalArgumentException.class, () -> PgValues.toParameter("not a timestamp", "timestamptz"));
    }
}
