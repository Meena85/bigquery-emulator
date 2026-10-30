package dev.meena.bigquery.init;

import java.util.ArrayList;
import java.util.List;

/** Splits a BigQuery SQL script into its statements. */
final class SqlScript {

    private SqlScript() {
    }

    /**
     * Splits on {@code ;} outside of comments, quoted strings and identifiers. Comments ({@code --}, {@code #},
     * and block comments) are removed; blank statements are dropped.
     */
    static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;
        int n = script.length();

        while (i < n) {
            char c = script.charAt(i);

            if (c == '-' && script.startsWith("--", i) || c == '#') {
                i = script.indexOf('\n', i);
                if (i < 0) break;
            } else if (script.startsWith("/*", i)) {
                int end = script.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                current.append(' ');
            } else if (c == '\'' || c == '"' || c == '`') {
                int end = endOfQuoted(script, i);
                current.append(script, i, end);
                i = end;
            } else if (c == ';') {
                add(statements, current);
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        add(statements, current);
        return statements;
    }

    /** Index just after the quoted string or identifier starting at {@code start} (triple quotes included). */
    private static int endOfQuoted(String script, int start) {
        char quote = script.charAt(start);
        boolean triple = quote != '`' && script.startsWith(String.valueOf(quote).repeat(3), start);
        String close = triple ? String.valueOf(quote).repeat(3) : String.valueOf(quote);

        int i = start + close.length();
        while (i < script.length()) {
            if (script.charAt(i) == '\\' && quote != '`') {
                i += 2;
            } else if (script.startsWith(close, i)) {
                return i + close.length();
            } else {
                i++;
            }
        }
        throw new IllegalArgumentException("Unterminated quote starting at: " + script.substring(start, Math.min(script.length(), start + 40)));
    }

    private static void add(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty())
            statements.add(statement);
        current.setLength(0);
    }
}
