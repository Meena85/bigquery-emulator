package dev.meena.bigquery.responses;

import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorsTest {

    private static Errors.ErrorResponse body(Response response) {
        return (Errors.ErrorResponse) response.getEntity();
    }

    @Test
    void errorEnvelopeMatchesBigQueryShape() {
        Response r = Errors.bigQueryError(Response.Status.NOT_FOUND, "Not found: Table p:d.t");
        assertEquals(404, r.getStatus());
        assertEquals(404, body(r).error().code());
        assertEquals("NOT_FOUND", body(r).error().status());
        assertEquals("notFound", body(r).error().errors().getFirst().reason());
        assertEquals("global", body(r).error().errors().getFirst().domain());
    }

    @Test
    void toErrorMapsExceptions() {
        SqlParseException parse = new SqlParseException("bad sql", SqlParserPos.ZERO, null, null, null);
        assertEquals(400, Errors.toError(parse).getStatus());
        assertEquals("invalidQuery", body(Errors.toError(parse)).error().errors().getFirst().reason());
        assertEquals(400, Errors.toError(new IllegalArgumentException("x")).getStatus());
        assertEquals(404, Errors.toError(new IllegalStateException("Not found: Table a")).getStatus());
        assertEquals(409, Errors.toError(new IllegalStateException("Already Exists: Table a")).getStatus());
        assertEquals(500, Errors.toError(new IllegalStateException("boom")).getStatus());
    }
}
