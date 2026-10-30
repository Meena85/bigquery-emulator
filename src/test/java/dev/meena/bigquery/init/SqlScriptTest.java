package dev.meena.bigquery.init;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlScriptTest {

    @Test
    void splitsOnSemicolonsAndDropsBlankStatements() {
        assertEquals(List.of("SELECT 1", "SELECT 2"), SqlScript.split(" SELECT 1 ;; SELECT 2;\n"));
        assertEquals(List.of(), SqlScript.split("  \n ; "));
    }

    @Test
    void semicolonsInStringsAndIdentifiersAreKept() {
        assertEquals(List.of("SELECT 'a;b'", "SELECT \"c;d\"", "SELECT `e;f`"),
                SqlScript.split("SELECT 'a;b'; SELECT \"c;d\"; SELECT `e;f`"));
    }

    @Test
    void backslashEscapesAndTripleQuotes() {
        assertEquals(List.of("SELECT 'it\\'s; ok'", "SELECT '''a ' ; b'''"),
                SqlScript.split("SELECT 'it\\'s; ok'; SELECT '''a ' ; b'''"));
    }

    @Test
    void commentsAreRemoved() {
        assertEquals(List.of("SELECT 1", "SELECT   2 AS x"),
                SqlScript.split("-- leading; comment\nSELECT 1; # hash; comment\nSELECT /* in; line */ 2 AS x -- trailing"));
    }

    @Test
    void commentMarkersInsideStringsAreKept() {
        assertEquals(List.of("SELECT '-- no', '# no', '/* no */'"), SqlScript.split("SELECT '-- no', '# no', '/* no */'"));
    }

    @Test
    void unterminatedQuoteIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> SqlScript.split("SELECT 'oops"));
    }
}
