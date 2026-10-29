package dev.meena.bigquery.fn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FnTest {

    @Test
    void toSchemaLowercasesAndJoins() {
        assertEquals("proj$ds", Fn.toSchema("Proj", "DS"));
        assertEquals("information_schema", Fn.toSchema("p", "information_schema"));
    }

    @Test
    void pgQuoteEscapesQuotes() {
        assertEquals("\"a\"\"b\"", Fn.pgQuote("a\"b"));
        assertEquals("\"\"", Fn.pgQuote(""));
    }

    @Test
    void removeSemiColonOnlyStripsTrailingOne() {
        assertEquals("SELECT 1", Fn.removeSemiColon("SELECT 1;"));
        assertEquals("SELECT ';' x", Fn.removeSemiColon("SELECT ';' x"));
    }
}
