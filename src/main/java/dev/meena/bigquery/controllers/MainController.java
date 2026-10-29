package dev.meena.bigquery.controllers;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.dialect.Bq2Psql;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

@Path("/")
public class MainController {

    @Inject
    Postgres postgres;

    @Inject
    Bq2Psql bq2Psql;

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
        if (query == null || query.isEmpty())
            return Response.ok(new TranslateResult(query, null, "'query' parameter is required")).build();

        try {
            Postgres.QueryResult result = postgres.query(bq2Psql.translate(query, projectId));
            return Response.ok(Objects.requireNonNullElseGet(result, () -> new TranslateResult(query, null, "Query failed"))).build();
        } catch (Exception e) {
            return Response.ok(new TranslateResult(query, null, e.getMessage())).build();
        }
    }

    @RegisterForReflection
    public record TranslateResult(String query, String result, String error) {
    }
}