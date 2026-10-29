package dev.meena.bigquery.dialect;

import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BqToPgDialectTest {

    private final Bq2Psql bq = new Bq2Psql();

    @Test
    void jsonPathSegments() {
        assertEquals(List.of(), BqToPgDialect.parseJsonPath("$"));
        assertEquals(List.of("a"), BqToPgDialect.parseJsonPath("$.a"));
        assertEquals(List.of("a", "b", "0", "c"), BqToPgDialect.parseJsonPath("$.a.b[0].c"));
        assertEquals(List.of("a", "b.c", "d e"), BqToPgDialect.parseJsonPath("$.a['b.c'][\"d e\"]"));
        assertEquals(List.of("a"), BqToPgDialect.parseJsonPath("lax $.a"));
        assertEquals(List.of("0", "1"), BqToPgDialect.parseJsonPath("$[0][1]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.b", "$.", "$..a", "$.*", "$[*]", "$[-1]", "$.a[", "$['a", "$.a b[", "$?(@.a)"})
    void unsupportedJsonPathsAreRejected(String path) {
        assertThrows(IllegalArgumentException.class, () -> BqToPgDialect.parseJsonPath(path));
    }

    @Test
    void textArrayEscapesQuotesAndBackslashes() {
        assertEquals("{\"a\",\"b\\\"c\",\"d\\\\e\"}", BqToPgDialect.toPgTextArray(List.of("a", "b\"c", "d\\e")));
    }

    @Test
    void jsonExtractReturnsJson() throws SqlParseException {
        assertEquals("SELECT (data) ::jsonb #> '{\"a\",\"b\",\"0\"}'\nFROM t",
                bq.translate("SELECT JSON_EXTRACT(data, '$.a.b[0]') FROM t"));
    }

    @Test
    void jsonValueReturnsText() throws SqlParseException {
        assertEquals("SELECT (data) ::jsonb #>> '{\"a\"}'\nFROM t",
                bq.translate("SELECT JSON_VALUE(data, '$.a') FROM t"));
    }

    @Test
    void jsonAliasesAndWholeDocument() throws SqlParseException {
        assertEquals("SELECT (data) ::jsonb #> '{}'\nFROM t", bq.translate("SELECT JSON_QUERY(data, '$') FROM t"));
        assertEquals("SELECT (data) ::jsonb #>> '{\"a\"}'\nFROM t", bq.translate("SELECT JSON_EXTRACT_SCALAR(data, '$.a') FROM t"));
    }

    @Test
    void jsonPathWithQuoteIsEscapedInSqlLiteral() throws SqlParseException {
        assertEquals("SELECT (data) ::jsonb #> '{\"it''s\"}'\nFROM t",
                bq.translate("SELECT JSON_EXTRACT(data, \"$['it\\'s']\") FROM t"));
    }

    @Test
    void jsonExtractOfExpressionAndInWhere() throws SqlParseException {
        assertEquals("SELECT id\nFROM t\nWHERE (data) ::jsonb #>> '{\"k\"}' = 'v'",
                bq.translate("SELECT id FROM t WHERE JSON_VALUE(data, '$.k') = 'v'"));
    }

    @Test
    void reservedWordsAndOddIdentifiersAreQuoted() throws SqlParseException {
        assertEquals("SELECT \"order\", \"1abc\", \"has space\", plain_name\nFROM t",
                bq.translate("SELECT `order`, `1abc`, `has space`, plain_name FROM t"));
    }
}
