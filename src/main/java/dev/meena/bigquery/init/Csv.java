package dev.meena.bigquery.init;

import java.util.ArrayList;
import java.util.List;

/** Minimal RFC 4180 CSV reader: quoted fields, doubled quotes, newlines inside quotes, optional BOM. */
final class Csv {

    private Csv() {
    }

    /**
     * Parses all records. An empty, unquoted field is {@code null} (BigQuery's default null marker);
     * an empty quoted field ({@code ""}) is an empty string.
     */
    static List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean wasQuoted = false;
        boolean anyContent = false;

        int i = text.startsWith("﻿") ? 1 : 0;
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"' && field.isEmpty() && !wasQuoted) {
                quoted = true;
                wasQuoted = true;
                anyContent = true;
            } else if (c == ',') {
                record.add(value(field, wasQuoted));
                field.setLength(0);
                wasQuoted = false;
                anyContent = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                if (anyContent) {
                    record.add(value(field, wasQuoted));
                    records.add(record);
                }
                record = new ArrayList<>();
                field.setLength(0);
                wasQuoted = false;
                anyContent = false;
            } else {
                field.append(c);
                anyContent = true;
            }
        }

        if (quoted)
            throw new IllegalArgumentException("Unterminated quoted field in CSV");
        if (anyContent) {
            record.add(value(field, wasQuoted));
            records.add(record);
        }
        return records;
    }

    private static String value(StringBuilder field, boolean wasQuoted) {
        return field.isEmpty() && !wasQuoted ? null : field.toString();
    }
}
