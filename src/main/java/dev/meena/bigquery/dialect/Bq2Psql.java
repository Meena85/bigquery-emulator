package dev.meena.bigquery.dialect;

import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.TableDataInsertAllRequest;
import dev.meena.bigquery.model.TableDataInsertAllRequestRowsInner;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;

@ApplicationScoped
public class Bq2Psql implements SqlSyntaxConverter {

    private static final Pattern DROP_TABLE_PATTERN = Pattern.compile("(?is)^\\s*drop\\s+table\\s+(if\\s+exists\\s+)?(.+)$");
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?is)^\\s*create\\s+(or\\s+replace\\s+)?table\\s+(if\\s+not\\s+exists\\s+)?(.+)$");
    private static final Set<String> INFORMATION_SCHEMA_IDENTIFIER_COLUMNS = Set.of(
            "table_catalog",
            "table_schema",
            "table_name",
            "column_name",
            "constraint_catalog",
            "constraint_schema",
            "constraint_name",
            "routine_catalog",
            "routine_schema",
            "routine_name",
            "specific_catalog",
            "specific_schema",
            "specific_name",
            "udt_catalog",
            "udt_schema",
            "udt_name"
    );
    private final SqlParser.Config parserConfig;

    public Bq2Psql() {
        // 1. Configure the parser to understand BigQuery syntax
        // BigQuery uses backticks (`) for identifiers and allows specific syntax
        parserConfig = SqlParser.config()
                .withConformance(SqlConformanceEnum.BIG_QUERY)
                .withLex(Lex.BIG_QUERY)
                .withCaseSensitive(false);
    }

    @Override
    public String translate(String bqSql) throws SqlParseException {
        if (bqSql == null) return null;

        bqSql = Fn.removeSemiColon(bqSql);
        String dropTableSql = translateDropTable(bqSql, null);
        if (dropTableSql != null) return dropTableSql;
        String createTableSql = translateCreateTable(bqSql, null);
        if (createTableSql != null) return createTableSql;

        // 2. Parse the BigQuery SQL into a SqlNode (AST)
        SqlParser parser = SqlParser.create(bqSql, parserConfig);

        SqlNode sqlNode = parser.parseQuery();

        // 3. Unparse the AST into PostgreSQL dialect
        // This automatically converts backticks to double quotes and handles quoting rules
        return rewriteTableIdentifiers(sqlNode, null).toSqlString(BqToPgDialect.INSTANCE).getSql();
    }

    @Override
    public String translate(String bqSql, String defaultProject) throws SqlParseException {
        if (bqSql == null) return null;

        bqSql = Fn.removeSemiColon(bqSql);

        String dropTableSql = translateDropTable(bqSql, defaultProject);
        if (dropTableSql != null) return dropTableSql;

        String createTableSql = translateCreateTable(bqSql, defaultProject);
        if (createTableSql != null) return createTableSql;

        SqlParser parser = SqlParser.create(bqSql, parserConfig);
        SqlNode sqlNode = parser.parseQuery();

        return rewriteTableIdentifiers(sqlNode, defaultProject)
                .toSqlString(BqToPgDialect.INSTANCE)
                .getSql();
    }

    private static String translateDropTable(String bqSql, String defaultProject) {
        Matcher matcher = DROP_TABLE_PATTERN.matcher(bqSql);
        if (!matcher.matches()) return null;

        String ifExists = matcher.group(1) == null ? "" : "IF EXISTS ";
        String tableAndSuffix = matcher.group(2).trim();
        IdentifierAndSuffix identifierAndSuffix = splitLeadingIdentifier(tableAndSuffix);
        SqlIdentifier rewritten = rewriteTableIdentifier(toSqlIdentifier(identifierAndSuffix.identifier()), defaultProject);

        StringBuilder sql = new StringBuilder("DROP TABLE ");
        sql.append(ifExists).append(rewritten.toSqlString(BqToPgDialect.INSTANCE).getSql());
        if (!identifierAndSuffix.suffix().isBlank()) {
            sql.append(" ").append(identifierAndSuffix.suffix().trim());
        }
        return sql.toString();
    }

    private static String translateCreateTable(String bqSql, String defaultProject) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(bqSql);
        if (!matcher.matches()) return null;

        String orReplace = matcher.group(1) == null ? "" : "OR REPLACE ";
        String ifNotExists = matcher.group(2) == null ? "" : "IF NOT EXISTS ";
        String tableAndSuffix = matcher.group(3).trim();
        IdentifierAndSuffix identifierAndSuffix = splitLeadingIdentifier(tableAndSuffix);
        SqlIdentifier rewritten = rewriteTableIdentifier(toSqlIdentifier(identifierAndSuffix.identifier()), defaultProject);

        StringBuilder sql = new StringBuilder("CREATE ");
        sql.append(orReplace).append("TABLE ").append(ifNotExists).append(rewritten.toSqlString(BqToPgDialect.INSTANCE).getSql());
        if (!identifierAndSuffix.suffix().isBlank()) {
            sql.append(" ").append(identifierAndSuffix.suffix().trim());
        }
        return sql.toString();
    }

    private static SqlIdentifier toSqlIdentifier(String rawIdentifier) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBackticks = false;

        for (int i = 0; i < rawIdentifier.length(); i++) {
            char c = rawIdentifier.charAt(i);
            if (c == '`') {
                inBackticks = !inBackticks;
                continue;
            }
            if (c == '.' && !inBackticks) {
                addIdentifierParts(parts, current);
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        addIdentifierParts(parts, current);
        return new SqlIdentifier(parts, SqlParserPos.ZERO);
    }

    private static void addIdentifierParts(List<String> parts, StringBuilder current) {
        String value = current.toString().trim();
        if (value.isEmpty()) return;
        for (String splitValue : value.split("\\.")) {
            String cleaned = splitValue.trim();
            if (!cleaned.isEmpty()) {
                parts.add(cleaned);
            }
        }
    }

    private static IdentifierAndSuffix splitLeadingIdentifier(String value) {
        StringBuilder identifier = new StringBuilder();
        boolean inBackticks = false;
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '`') {
                inBackticks = !inBackticks;
                identifier.append(c);
                i++;
                continue;
            }
            if (!inBackticks && Character.isWhitespace(c)) {
                break;
            }
            identifier.append(c);
            i++;
        }
        return new IdentifierAndSuffix(identifier.toString().trim(), value.substring(i).trim());
    }

    private record IdentifierAndSuffix(String identifier, String suffix) {
    }

    private static SqlNode rewriteTableIdentifiers(SqlNode sqlNode, String defaultProject) {
        return sqlNode.accept(new SqlSelectShuttle(defaultProject));
    }

    private static SqlNode rewriteFromNode(SqlNode from, String defaultProject) {
        if (from instanceof SqlIdentifier identifier) {
            return rewriteTableIdentifier(identifier, defaultProject);
        }

        if (from instanceof SqlJoin join) {
            join.setLeft(rewriteFromNode(join.getLeft(), defaultProject));
            join.setRight(rewriteFromNode(join.getRight(), defaultProject));
            return join;
        }

        if (from instanceof SqlCall call && call.getKind() == SqlKind.AS) {
            SqlNode table = call.operand(0);

            // Rewrite only the table expression. Do not rewrite operand 1,
            // because operand 1 is the alias.
            call.setOperand(0, rewriteFromNode(table, defaultProject));
            return call;
        }

        if (from instanceof SqlCall call) {
            return call.accept(new SqlSelectShuttle(defaultProject));
        }

        return from;
    }

    private static SqlIdentifier rewriteTableIdentifier(SqlIdentifier identifier, String defaultProject) {
        if (identifier.names.size() >= 2) {
            String schema = identifier.names.get(identifier.names.size() - 2);
            String table = identifier.names.get(identifier.names.size() - 1);
            if ("information_schema".equalsIgnoreCase(schema)) {
                return new SqlIdentifier(
                        List.of("information_schema", table.toLowerCase()),
                        identifier.getParserPosition()
                );
            }
        }

        if (identifier.names.size() == 3) {
            String project = identifier.names.get(0);
            String dataset = identifier.names.get(1);
            String table = identifier.names.get(2);

            return new SqlIdentifier(
                    List.of(Fn.toSchema(project, dataset), table),
                    identifier.getParserPosition()
            );
        }

        if (identifier.names.size() == 2 && defaultProject != null && !defaultProject.isBlank()) {
            String dataset = identifier.names.get(0);
            String table = identifier.names.get(1);

            return new SqlIdentifier(
                    List.of(Fn.toSchema(defaultProject, dataset), table),
                    identifier.getParserPosition()
            );
        }

        return identifier;
    }

    public record InsertAll(
            String schema,
            String table,
            List<String> columns,
            String preparedSql,
            List<Object[]> rows
    ) {
    }

    public InsertAll insertAll(TableDataInsertAllRequest req, String projectId, String schema, String table) {
        if (req == null || req.getRows() == null) return null;
        schema = Fn.toSchema(projectId, schema);

        Map<String, Class<?>> columns = new LinkedHashMap<>();
        for (TableDataInsertAllRequestRowsInner row : req.getRows()) {
            if (row.getJson() == null) continue;

            for (Map.Entry<String, Object> entry : row.getJson().entrySet()) {
                if (entry.getValue() == null) continue;
                columns.putIfAbsent(entry.getKey(), entry.getValue().getClass());
            }
        }

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("At least one non-null JSON field is required");
        }

        List<String> columnNames = new ArrayList<>(columns.keySet());

        String preparedSql = "INSERT INTO " + quoteIdentifier(schema) + "." + quoteIdentifier(table) +
                " (" + columnNames.stream().map(Bq2Psql::quoteIdentifier).collect(joining(", ")) + ")" +
                " VALUES (" + String.join(", ", nCopies(columnNames.size(), "?")) + ")" +
                " ON CONFLICT DO NOTHING;";

        List<Object[]> insert = new ArrayList<>(req.getRows().size());
        for (TableDataInsertAllRequestRowsInner row : req.getRows()) {
            int i = 0;
            Object[] insertRow = new Object[columnNames.size()];
            for (String columnName : columnNames) {
                insertRow[i++] = row.getJson() == null ? null : row.getJson().getOrDefault(columnName, null);
            }
            insert.add(insertRow);
        }

        return new InsertAll(schema, table, columnNames, preparedSql, insert);
    }

    private static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier is required");
        }

        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static boolean referencesInformationSchema(SqlNode from) {
        if (from == null) return false;
        if (from instanceof SqlIdentifier identifier) {
            return identifier.names.stream().anyMatch("information_schema"::equalsIgnoreCase);
        }
        if (from instanceof SqlJoin join) {
            return referencesInformationSchema(join.getLeft()) || referencesInformationSchema(join.getRight());
        }
        if (from instanceof SqlCall call) {
            return call.getOperandList().stream().anyMatch(Bq2Psql::referencesInformationSchema);
        }
        return false;
    }

    private static boolean isInformationSchemaIdentifierColumn(SqlNode node) {
        if (node instanceof SqlIdentifier identifier && !identifier.names.isEmpty()) {
            String colName = identifier.names.getLast().toLowerCase(Locale.ROOT);
            return INFORMATION_SCHEMA_IDENTIFIER_COLUMNS.contains(colName);
        }
        return false;
    }

    private static SqlNode wrapLower(SqlNode node) {
        if (node instanceof SqlCall call && "LOWER".equalsIgnoreCase(call.getOperator().getName())) {
            return node;
        }
        return SqlStdOperatorTable.LOWER.createCall(SqlParserPos.ZERO, node);
    }

    private static class InformationSchemaPredicateShuttle extends SqlShuttle {
        @Override
        public SqlNode visit(SqlCall call) {
            SqlKind kind = call.getKind();
            if ((kind == SqlKind.EQUALS || kind == SqlKind.NOT_EQUALS) && call.operandCount() == 2) {
                SqlNode left = call.operand(0);
                SqlNode right = call.operand(1);
                if (isInformationSchemaIdentifierColumn(left) || isInformationSchemaIdentifierColumn(right)) {
                    call.setOperand(0, wrapLower(left));
                    call.setOperand(1, wrapLower(right));
                    return call;
                }
            }
            if ((kind == SqlKind.IN || kind == SqlKind.NOT_IN) && call.operandCount() == 2) {
                SqlNode left = call.operand(0);
                SqlNode right = call.operand(1);
                if (isInformationSchemaIdentifierColumn(left) && right instanceof SqlNodeList list) {
                    call.setOperand(0, wrapLower(left));
                    for (int i = 0; i < list.size(); i++) {
                        list.set(i, wrapLower(list.get(i)));
                    }
                    return call;
                }
            }
            return super.visit(call);
        }
    }

    private static class SqlSelectShuttle extends SqlShuttle {

        private final String defaultProject;

        public SqlSelectShuttle(String defaultProject) {
            this.defaultProject = defaultProject;
        }

        @Override
        public SqlNode visit(SqlCall call) {
            if (call instanceof SqlSelect select) {
                SqlNode from = select.getFrom();
                if (from != null) {
                    boolean isInfoSchema = referencesInformationSchema(from);
                    select.setFrom(rewriteFromNode(from, defaultProject));
                    if (isInfoSchema && select.getWhere() != null) {
                        select.setWhere(select.getWhere().accept(new InformationSchemaPredicateShuttle()));
                    }
                }
            }
            return super.visit(call);
        }
    }
}
