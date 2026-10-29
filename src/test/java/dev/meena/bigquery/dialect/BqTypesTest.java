package dev.meena.bigquery.dialect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BqTypesTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "INT64|BIGINT", "INT|BIGINT", "SMALLINT|BIGINT", "INTEGER|BIGINT", "BIGINT|BIGINT", "TINYINT|BIGINT", "BYTEINT|BIGINT",
            "FLOAT64|DOUBLE PRECISION", "FLOAT|DOUBLE PRECISION",
            "NUMERIC|NUMERIC(38,9)", "DECIMAL|NUMERIC(38,9)", "NUMERIC(10)|NUMERIC(10)", "NUMERIC(10, 2)|NUMERIC(10,2)",
            "BIGNUMERIC|NUMERIC", "BIGDECIMAL|NUMERIC", "BIGNUMERIC(50, 10)|NUMERIC(50,10)",
            "BOOL|BOOLEAN", "BOOLEAN|BOOLEAN",
            "STRING|TEXT", "STRING(100)|TEXT",
            "BYTES|BYTEA", "BYTES(16)|BYTEA",
            "DATE|DATE", "TIME|TIME", "DATETIME|TIMESTAMP", "TIMESTAMP|TIMESTAMPTZ",
            "GEOGRAPHY|TEXT", "JSON|JSONB", "INTERVAL|INTERVAL",
            "STRUCT<a INT64, b STRING>|JSONB", "RECORD|JSONB",
            "RANGE<DATE>|DATERANGE", "RANGE<DATETIME>|TSRANGE", "RANGE<TIMESTAMP>|TSTZRANGE",
            "ARRAY<INT64>|BIGINT[]", "ARRAY<STRING>|TEXT[]", "ARRAY<TIMESTAMP>|TIMESTAMPTZ[]",
            "ARRAY<STRUCT<a INT64>>|JSONB[]", "ARRAY<NUMERIC>|NUMERIC(38,9)[]",
            "int64|BIGINT", "  Timestamp |TIMESTAMPTZ"
    })
    void bigQueryToPostgres(String bigQuery, String postgres) {
        assertEquals(postgres, BqTypes.toPg(bigQuery));
    }

    @Test
    void unknownAndMissingTypesFallBackToText() {
        assertEquals("TEXT", BqTypes.toPg(null));
        assertEquals("TEXT", BqTypes.toPg(""));
        assertEquals("TEXT", BqTypes.toPg("SOMETHING_NEW"));
    }

    @ParameterizedTest(name = "{0} -> {1} repeated={2}")
    @CsvSource(delimiter = '|', value = {
            "int8|INTEGER|false", "int4|INTEGER|false", "int2|INTEGER|false",
            "float8|FLOAT|false", "float4|FLOAT|false", "numeric|NUMERIC|false",
            "bool|BOOLEAN|false", "text|STRING|false", "varchar|STRING|false",
            "bytea|BYTES|false", "date|DATE|false", "time|TIME|false",
            "timestamp|DATETIME|false", "timestamptz|TIMESTAMP|false",
            "json|JSON|false", "jsonb|JSON|false", "interval|INTERVAL|false",
            "daterange|STRING|false", "uuid|STRING|false",
            "_int8|INTEGER|true", "_text|STRING|true", "_timestamptz|TIMESTAMP|true", "_bytea|BYTES|true"
    })
    void postgresToBigQuery(String postgres, String bigQuery, boolean repeated) {
        assertEquals(new BqTypes.BqType(bigQuery, repeated), BqTypes.fromPg(postgres));
    }
}
