package dev.meena.bigquery.fn;

import dev.meena.bigquery.db.Postgres;
import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FnTest {

    private static Fn.ErrorResponse body(Response response) {
        return (Fn.ErrorResponse) response.getEntity();
    }

    @Test
    void errorEnvelopeMatchesBigQueryShape() {
        Response r = Fn.bigQueryError(Response.Status.NOT_FOUND, "Not found: Table p:d.t");
        assertEquals(404, r.getStatus());
        assertEquals(404, body(r).error().code());
        assertEquals("NOT_FOUND", body(r).error().status());
        assertEquals("notFound", body(r).error().errors().getFirst().reason());
        assertEquals("global", body(r).error().errors().getFirst().domain());
    }

    @Test
    void toErrorMapsExceptions() {
        SqlParseException parse = new SqlParseException("bad sql", SqlParserPos.ZERO, null, null, null);
        assertEquals(400, Fn.toError(parse).getStatus());
        assertEquals("invalidQuery", body(Fn.toError(parse)).error().errors().getFirst().reason());
        assertEquals(400, Fn.toError(new IllegalArgumentException("x")).getStatus());
        assertEquals(404, Fn.toError(new IllegalStateException("Not found: Table a")).getStatus());
        assertEquals(409, Fn.toError(new IllegalStateException("Already Exists: Table a")).getStatus());
        assertEquals(500, Fn.toError(new IllegalStateException("boom")).getStatus());
    }

    @Test
    void queryResponseHasSchemaRowsAndKind() {
        var result = new Postgres.QueryResult(
                List.of(new Postgres.Column("n", Types.BIGINT, "int8"), new Postgres.Column("s", Types.VARCHAR, "text")),
                List.of(Arrays.asList(1L, "a"), Arrays.asList(2L, null)));

        var response = Fn.queryResponse("p", "job1", "US", result);
        assertEquals("bigquery#queryResponse", response.getKind());
        assertTrue(response.getJobComplete());
        assertEquals("2", response.getTotalRows());
        assertEquals("job1", response.getJobReference().getJobId());
        assertEquals("INTEGER", response.getSchema().getFields().getFirst().getType());
        assertEquals("1", response.getRows().getFirst().getF().getFirst().getV());
        assertNull(response.getRows().get(1).getF().get(1).getV());
    }

    @Test
    void toSchemaLowercasesAndJoins() {
        assertEquals("proj$ds", Fn.toSchema("Proj", "DS"));
        assertEquals("information_schema", Fn.toSchema("p", "information_schema"));
    }
}
