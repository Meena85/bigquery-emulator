package dev.meena.bigquery.dialect;

import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.TableDataInsertAllRequest;
import dev.meena.bigquery.model.TableDataInsertAllRequestRowsInner;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;

@ApplicationScoped
public class Bq2Psql implements SqlSyntaxConverter {

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
        // 2. Parse the BigQuery SQL into a SqlNode (AST)
        SqlParser parser = SqlParser.create(bqSql, parserConfig);
        SqlNode sqlNode = parser.parseQuery();

        // 3. Unparse the AST into PostgreSQL dialect
        // This automatically converts backticks to double quotes and handles quoting rules
        return rewriteTableIdentifiers(sqlNode, null).toSqlString(BqToPgDialect.INSTANCE).getSql();
    }

    @Override
    public String translate(String bqSql, String defaultProject) throws SqlParseException {
        SqlParser parser = SqlParser.create(bqSql, parserConfig);
        SqlNode sqlNode = parser.parseQuery();

        return rewriteTableIdentifiers(sqlNode, defaultProject)
                .toSqlString(BqToPgDialect.INSTANCE)
                .getSql();
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
        if (identifier.names.size() == 3) {
            String project = identifier.names.get(0);
            String dataset = identifier.names.get(1);
            String table = identifier.names.get(2);

            return new SqlIdentifier(
                    List.of(project.toLowerCase() + "$" + dataset.toLowerCase(), table),
                    identifier.getParserPosition()
            );
        }

        if (identifier.names.size() == 2 && defaultProject != null && !defaultProject.isBlank()) {
            String dataset = identifier.names.get(0);
            String table = identifier.names.get(1);

            return new SqlIdentifier(
                    List.of(defaultProject.toLowerCase() + "$" + dataset.toLowerCase(), table),
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

    private static class SqlSelectShuttle extends SqlShuttle {

        private final String defaultProject;

        public SqlSelectShuttle(String defaultProject) {
            this.defaultProject = defaultProject;
        }

        @Override
        public SqlNode visit(SqlCall call) {
            if (call instanceof SqlSelect select) {
                SqlNode from = select.getFrom();
                if (from != null)
                    select.setFrom(rewriteFromNode(from, defaultProject));
            }
            return super.visit(call);
        }
    }
}
