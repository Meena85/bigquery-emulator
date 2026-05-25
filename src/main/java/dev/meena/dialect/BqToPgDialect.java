package dev.meena.dialect;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

public class BqToPgDialect extends PostgresqlSqlDialect {

    public static final Context DEFAULT_CONTEXT = PostgresqlSqlDialect.DEFAULT_CONTEXT;
    public static final BqToPgDialect INSTANCE = new BqToPgDialect(DEFAULT_CONTEXT);

    public BqToPgDialect(Context context) {
        super(context);
    }

    @Override
    public void unparseCall(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        String name = call.getOperator().getName().toUpperCase();

        convertJsonExtract(writer, call, leftPrec, rightPrec, name);

        super.unparseCall(writer, call, leftPrec, rightPrec);
    }

    static String translateJsonPath(String bqPath) {
        // Simple regex-based cleanup for a hobby project:
        // 1. Strip '$.' and quotes
        // 2. Replace '.' and '[]' segments with commas
        return bqPath.replace("'", "")
                .replace("$.", "")
                .replaceAll("\\[(\\d+)\\]", ".$1") // [0] -> .0
                .replace(".", ", ");
    }

    static void convertJsonExtract(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec, String name) {
        if (name.equals("JSON_EXTRACT") || name.equals("JSON_VALUE")) {
            // BigQuery: JSON_EXTRACT(json_col, '$.path')
            // Postgres: json_col::jsonb #> '{path}'

            SqlNode jsonExpr = call.operand(0);
            SqlNode pathExpr = call.operand(1);

            jsonExpr.unparse(writer, leftPrec, rightPrec);
            writer.print("::jsonb");

            // Choose operator: #> returns jsonb, #>> returns text (for JSON_VALUE)
            writer.print(name.equals("JSON_VALUE") ? " #>> " : " #> ");

            // Convert '$.parts[0].text' to Postgres format '{parts, 0, text}'
            String pgPath = translateJsonPath(pathExpr.toString());
            writer.literal("'{" + pgPath + "}'");
        }
    }
}