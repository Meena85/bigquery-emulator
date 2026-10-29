package dev.meena.bigquery.responses;

import dev.meena.bigquery.db.Postgres;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryResponsesTest {

    @Test
    void queryResponseHasSchemaRowsAndKind() {
        var result = new Postgres.QueryResult(
                List.of(new Postgres.Column("n", Types.BIGINT, "int8"), new Postgres.Column("s", Types.VARCHAR, "text")),
                List.of(Arrays.asList(1L, "a"), Arrays.asList(2L, null)));

        var response = QueryResponses.queryResponse("p", "job1", "US", result);
        assertEquals("bigquery#queryResponse", response.getKind());
        assertTrue(response.getJobComplete());
        assertEquals("2", response.getTotalRows());
        assertEquals("job1", response.getJobReference().getJobId());
        assertEquals("INTEGER", response.getSchema().getFields().getFirst().getType());
        assertEquals("1", response.getRows().getFirst().getF().getFirst().getV());
        assertNull(response.getRows().get(1).getF().get(1).getV());
    }

    @Test
    void schemaReportsRepeatedAndTemporalTypes() {
        var schema = QueryResponses.tableSchema(List.of(
                new Postgres.Column("a", Types.ARRAY, "_text"),
                new Postgres.Column("b", Types.TIMESTAMP, "timestamptz"),
                new Postgres.Column("c", Types.TIMESTAMP, "timestamp"),
                new Postgres.Column("d", Types.BINARY, "bytea")));
        assertEquals("REPEATED", schema.getFields().get(0).getMode());
        assertEquals("STRING", schema.getFields().get(0).getType());
        assertEquals("TIMESTAMP", schema.getFields().get(1).getType());
        assertEquals("DATETIME", schema.getFields().get(2).getType());
        assertEquals("BYTES", schema.getFields().get(3).getType());
    }
}
