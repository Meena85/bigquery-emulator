package dev.meena.bigquery.controllers;

import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.Bq2Psql;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;
import org.postgresql.util.PSQLException;

@Path("/")
public class MainController {

    @Inject
    Postgres postgres;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "BigQuery Emulator";
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response translate(
            @FormParam("query") String query,
            @FormParam("projectId") String projectId) {
        if (query == null || query.isEmpty()) {
            return Response.ok(new QueryResult(query, null, "'query' parameter is required")).build();
        }

        var bq2Psql = new Bq2Psql();
        try {
            Postgres.QueryResult result = postgres.query(bq2Psql.translate(query, projectId));
            if (result == null) {
                return Response.ok(new QueryResult(query, null, "Query failed")).build();
            }
            return Response.ok(Fn.toSimpleJsonArray(result).toJSONString()).build();
        } catch (Exception e) {
            return Response.ok(new QueryResult(query, null, e.getMessage())).build();
        }
    }

    public record QueryResult(String query, String result, String error) {}
}