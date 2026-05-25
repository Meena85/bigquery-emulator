package dev.meena.controllers;

import dev.meena.dialect.Bq2Psql;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.calcite.sql.parser.SqlParseException;

@Path("/")
public class MainController {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String translate(@FormParam("query") String query) {
        if (query == null || query.isEmpty()) {
            return "'query' parameter is required";
        }

        var bq = new Bq2Psql();
        try {
            return bq.translate(query);
        } catch (SqlParseException e) {
            return e.getMessage();
        }
    }
}
