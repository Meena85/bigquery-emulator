package dev.meena.bigquery.responses;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.Response;
import org.apache.calcite.sql.parser.SqlParseException;

import java.util.List;

/**
 * Builds BigQuery's standard error responses.
 */
public final class Errors {

    private Errors() {
    }

    /**
     * BigQuery's standard error envelope: {"error": {code, message, status, errors: [...]}}.
     */
    @RegisterForReflection(ignoreNested = false)
    public record ErrorResponse(Error error) {
        public record Error(int code, String message, String status, List<Detail> errors) {
        }

        public record Detail(String message, String domain, String reason, String locationType) {
        }
    }

    public static Response bigQueryError(Response.Status status, String message) {
        return bigQueryError(status, message, defaultReason(status));
    }

    public static Response bigQueryError(Response.Status status, String message, String reason) {
        var detail = new ErrorResponse.Detail(message, "global", reason, "other");
        var error = new ErrorResponse.Error(status.getStatusCode(), message, googleStatus(status), List.of(detail));
        return Response.status(status).entity(new ErrorResponse(error)).build();
    }

    /**
     * Map an exception to a BigQuery error response: SQL syntax problems are 400 {@code invalidQuery},
     * "Not found: ..." is 404, "Already Exists: ..." is 409 and anything else is a 500.
     */
    public static Response toError(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return switch (e) {
            case SqlParseException ignored -> bigQueryError(Response.Status.BAD_REQUEST, message, "invalidQuery");
            case IllegalArgumentException ignored -> bigQueryError(Response.Status.BAD_REQUEST, message, "invalid");
            default -> {
                if (message.startsWith("Not found")) yield bigQueryError(Response.Status.NOT_FOUND, message);
                if (message.startsWith("Already Exists")) yield bigQueryError(Response.Status.CONFLICT, message);
                yield bigQueryError(Response.Status.INTERNAL_SERVER_ERROR, message);
            }
        };
    }

    private static String defaultReason(Response.Status status) {
        return switch (status) {
            case BAD_REQUEST -> "invalid";
            case NOT_FOUND -> "notFound";
            case CONFLICT -> "duplicate";
            case NOT_IMPLEMENTED -> "notImplemented";
            default -> "backendError";
        };
    }

    private static String googleStatus(Response.Status status) {
        return switch (status) {
            case BAD_REQUEST -> "INVALID_ARGUMENT";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "ALREADY_EXISTS";
            case NOT_IMPLEMENTED -> "UNIMPLEMENTED";
            default -> "INTERNAL";
        };
    }
}
