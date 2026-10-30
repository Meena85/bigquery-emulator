package dev.meena.bigquery.init;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvTest {

    @Test
    void plainRecordsWithAnyLineEnding() {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2"), List.of("3", "4")), Csv.parse("a,b\n1,2\r\n3,4"));
        assertEquals(List.of(List.of("a")), Csv.parse("a\n\n"));
    }

    @Test
    void quotedFieldsKeepCommasQuotesAndNewlines() {
        assertEquals(List.of(List.of("x, y", "say \"hi\"", "two\nlines")), Csv.parse("\"x, y\",\"say \"\"hi\"\"\",\"two\nlines\""));
    }

    @Test
    void emptyUnquotedFieldIsNullButQuotedIsEmptyString() {
        assertEquals(List.of(Arrays.asList(null, "", "x", null)), Csv.parse(",\"\",x,"));
    }

    @Test
    void byteOrderMarkIsIgnored() {
        assertEquals(List.of(List.of("id", "n")), Csv.parse("﻿id,n"));
    }

    @Test
    void unterminatedQuoteIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("a,\"oops"));
    }
}
