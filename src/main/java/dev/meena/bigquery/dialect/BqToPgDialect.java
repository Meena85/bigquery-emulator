package dev.meena.bigquery.dialect;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Converts BigQuery SQL to Postgres SQL by customizing how Calcite unparses the parsed query.
 * <p>
 * BigQuery's JSON functions are rewritten to jsonb path operators:
 * <ul>
 *   <li>{@code JSON_EXTRACT} / {@code JSON_QUERY(json, '$.a.b[0]')} becomes {@code (json)::jsonb #> '{"a","b","0"}'}</li>
 *   <li>{@code JSON_VALUE} / {@code JSON_EXTRACT_SCALAR(json, '$.a.b[0]')} becomes {@code (json)::jsonb #>> '{"a","b","0"}'}</li>
 * </ul>
 * Only plain paths (member names and array indexes) are supported; wildcards, filters and recursive
 * descent are rejected with an {@link IllegalArgumentException} rather than silently mistranslated.
 */
public class BqToPgDialect extends PostgresqlSqlDialect {

    public static final Context DEFAULT_CONTEXT = PostgresqlSqlDialect.DEFAULT_CONTEXT;
    public static final BqToPgDialect INSTANCE = new BqToPgDialect(DEFAULT_CONTEXT);

    private static final Set<String> JSON_AS_JSON = Set.of("JSON_EXTRACT", "JSON_QUERY");
    private static final Set<String> JSON_AS_TEXT = Set.of("JSON_VALUE", "JSON_EXTRACT_SCALAR");

    private static final Pattern PLAIN_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern ARRAY_INDEX = Pattern.compile("\\d+");

    /** Postgres reserved words that cannot be used as unquoted identifiers. */
    private static final Set<String> RESERVED_WORDS = Set.of(
            "all", "analyse", "analyze", "and", "any", "array", "as", "asc", "asymmetric", "both", "case", "cast",
            "check", "collate", "column", "constraint", "create", "current_catalog", "current_date", "current_role",
            "current_time", "current_timestamp", "current_user", "default", "deferrable", "desc", "distinct", "do",
            "else", "end", "except", "false", "fetch", "for", "foreign", "from", "grant", "group", "having", "in",
            "initially", "intersect", "into", "lateral", "leading", "limit", "localtime", "localtimestamp", "not",
            "null", "offset", "on", "only", "or", "order", "placing", "primary", "references", "returning", "select",
            "session_user", "some", "symmetric", "table", "then", "to", "trailing", "true", "union", "unique", "user",
            "using", "variadic", "when", "where", "window", "with");

    public BqToPgDialect(Context context) {
        super(context);
    }

    @Override
    public void unparseCall(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        String name = call.getOperator().getName().toUpperCase(Locale.ROOT);

        if (JSON_AS_JSON.contains(name) || JSON_AS_TEXT.contains(name)) {
            unparseJsonPath(writer, call, JSON_AS_TEXT.contains(name));
            return;
        }

        super.unparseCall(writer, call, leftPrec, rightPrec);
    }

    /**
     * Identifiers made of letters, digits and underscores are left unquoted (Postgres folds them to lower
     * case, matching BigQuery's case-insensitive column names); anything else, and reserved words, is quoted.
     */
    @Override
    protected boolean identifierNeedsQuote(String val) {
        return !PLAIN_IDENTIFIER.matcher(val).matches() || RESERVED_WORDS.contains(val.toLowerCase(Locale.ROOT));
    }

    private static void unparseJsonPath(SqlWriter writer, SqlCall call, boolean asText) {
        String function = call.getOperator().getName();
        if (call.operandCount() < 2)
            throw new IllegalArgumentException(function + " requires a JSON value and a JSON path");
        if (!(call.operand(1) instanceof SqlLiteral path) || path.getValueAs(String.class) == null)
            throw new IllegalArgumentException(function + " is only supported with a string literal path");

        SqlNode json = call.operand(0);
        SqlWriter.Frame frame = writer.startList("(", ")");
        json.unparse(writer, 0, 0);
        writer.endList(frame);
        writer.print("::jsonb");
        writer.print(asText ? " #>> " : " #> ");
        writer.print(toSqlLiteral(toPgTextArray(parseJsonPath(path.getValueAs(String.class)))));
    }

    /**
     * Splits a BigQuery JSON path into its member names and array indexes: {@code $.a['b.c'][0]} gives
     * {@code [a, b.c, 0]}. {@code $} alone selects the whole document and gives an empty list.
     */
    static List<String> parseJsonPath(String path) {
        String p = path.trim();
        if (p.regionMatches(true, 0, "lax ", 0, 4)) p = p.substring(4).trim();
        else if (p.regionMatches(true, 0, "strict ", 0, 7)) p = p.substring(7).trim();

        if (!p.startsWith("$"))
            throw new IllegalArgumentException("Invalid JSON path, it must start with '$': " + path);

        List<String> segments = new ArrayList<>();
        int i = 1;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (c == '.') {
                if (p.startsWith("..", i))
                    throw new IllegalArgumentException("Recursive descent is not supported in JSON path: " + path);
                int start = ++i;
                while (i < p.length() && p.charAt(i) != '.' && p.charAt(i) != '[') i++;
                String member = p.substring(start, i);
                if (member.isEmpty() || member.equals("*"))
                    throw new IllegalArgumentException("Unsupported member in JSON path: " + path);
                segments.add(member);
            } else if (c == '[') {
                i++;
                if (i < p.length() && (p.charAt(i) == '\'' || p.charAt(i) == '"')) {
                    char quote = p.charAt(i++);
                    StringBuilder member = new StringBuilder();
                    while (i < p.length() && p.charAt(i) != quote) {
                        if (p.charAt(i) == '\\' && i + 1 < p.length()) i++; // backslash escapes the next character
                        member.append(p.charAt(i++));
                    }
                    if (i >= p.length())
                        throw new IllegalArgumentException("Unterminated quoted member in JSON path: " + path);
                    segments.add(member.toString());
                    i++;
                    if (i >= p.length() || p.charAt(i) != ']')
                        throw new IllegalArgumentException("Expected ']' in JSON path: " + path);
                    i++;
                } else {
                    int end = p.indexOf(']', i);
                    if (end < 0)
                        throw new IllegalArgumentException("Unterminated '[' in JSON path: " + path);
                    String index = p.substring(i, end).trim();
                    if (!ARRAY_INDEX.matcher(index).matches())
                        throw new IllegalArgumentException("Unsupported array subscript '" + index + "' in JSON path: " + path);
                    segments.add(index);
                    i = end + 1;
                }
            } else {
                throw new IllegalArgumentException("Unexpected '" + c + "' in JSON path: " + path);
            }
        }
        return segments;
    }

    /** Postgres text array literal with every element quoted: {@code {"a","b c"}}. */
    static String toPgTextArray(List<String> elements) {
        StringBuilder array = new StringBuilder("{");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) array.append(',');
            array.append('"').append(elements.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return array.append('}').toString();
    }

    private static String toSqlLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
