package dev.meena.controllers;

import dev.meena.controllers.fn.Fn;
import dev.meena.db.Postgres;
import dev.meena.dialect.Bq2Psql;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.calcite.sql.parser.SqlParseException;

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
    public String translate(@FormParam("query") String query) {
        if (query == null || query.isEmpty()) {
            return "{\"message\":\"'query' parameter is required\"}";
        }

        var bq2Psql = new Bq2Psql();
        try {
            Postgres.QueryResult result = postgres.query(bq2Psql.translate(query));
            if (result == null) {
                return "[]";
            }
            return Fn.toSimpleJsonArray(result).toJSONString();
        } catch (SqlParseException e) {
            return e.getMessage();
        }
    }
}