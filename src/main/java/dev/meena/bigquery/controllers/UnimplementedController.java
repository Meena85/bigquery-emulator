package dev.meena.bigquery.controllers;

import dev.meena.bigquery.responses.UnimplementedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import java.util.logging.Logger;

/**
 * Catch-all for BigQuery v2 endpoints that have no controller yet: logs the call and answers 501.
 */
@Path("/bigquery/v2/{path: .*}")
@Produces(MediaType.APPLICATION_JSON)
public class UnimplementedController {

    private static final Logger log = Logger.getLogger("UnimplementedController");

    @GET
    public Response get(@PathParam("path") String path, @Context UriInfo uriInfo) {
        return unimplemented("GET", path, uriInfo, null);
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    public Response post(@PathParam("path") String path, @Context UriInfo uriInfo, String body) {
        return unimplemented("POST", path, uriInfo, body);
    }

    @PUT
    @Consumes(MediaType.WILDCARD)
    public Response put(@PathParam("path") String path, @Context UriInfo uriInfo, String body) {
        return unimplemented("PUT", path, uriInfo, body);
    }

    @PATCH
    @Consumes(MediaType.WILDCARD)
    public Response patch(@PathParam("path") String path, @Context UriInfo uriInfo, String body) {
        return unimplemented("PATCH", path, uriInfo, body);
    }

    @DELETE
    public Response delete(@PathParam("path") String path, @Context UriInfo uriInfo) {
        return unimplemented("DELETE", path, uriInfo, null);
    }

    private static Response unimplemented(String method, String path, UriInfo uriInfo, String body) {
        String params = uriInfo.getQueryParameters().toString();
        log.warning("Unimplemented BigQuery endpoint called:\n [" + method + "] /bigquery/v2/" + path
                + "\n  parameters: " + params + "\n  body: " + body);

        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(new UnimplementedResponse(
                        "BigQuery endpoint is not implemented yet",
                        uriInfo.getRequestUri().toString(),
                        "/bigquery/v2/" + path,
                        params,
                        body))
                .build();
    }
}
