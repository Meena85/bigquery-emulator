package dev.meena.dialect;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

public class Bq2Psql {

    public String translate(String bqSql) throws SqlParseException {
        // 1. Configure the parser to understand BigQuery syntax
        // BigQuery uses backticks (`) for identifiers and allows specific syntax
        SqlParser.Config parserConfig = SqlParser.config()
                .withConformance(SqlConformanceEnum.BIG_QUERY)
                .withLex(Lex.BIG_QUERY)
                .withCaseSensitive(false);

        // 2. Parse the BigQuery SQL into a SqlNode (AST)
        SqlParser parser = SqlParser.create(bqSql, parserConfig);
        SqlNode sqlNode = parser.parseQuery();

        // 3. Unparse the AST into PostgreSQL dialect
        // This automatically converts backticks to double quotes and handles quoting rules
        return sqlNode.toSqlString(BqToPgDialect.INSTANCE).getSql();
    }
}
