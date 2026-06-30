package dev.meena.bigquery.dialect;

import org.apache.calcite.sql.parser.SqlParseException;

public interface SqlSyntaxConverter {

    String translate(String bqSql) throws SqlParseException;
    String translate(String bqSql, String projectName) throws SqlParseException;
}
