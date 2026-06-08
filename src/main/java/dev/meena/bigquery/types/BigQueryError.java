package dev.meena.bigquery.types;

import jakarta.ws.rs.core.Response;

import java.util.List;

public class BigQueryError {

    public Err error;
    public List<ErrDetail> errors;

    record Err(String message, int code, String status) {
    }

    record ErrDetail(String message, String reason, String locationType) {
    }

    public BigQueryError(String message, Response.Status status) {
        this.error = new Err(message, status.getStatusCode(), status.name());
        this.errors = List.of(new ErrDetail(
                message,
                status == Response.Status.BAD_REQUEST ? "invalidQuery" : "backendError",
                "other"));
    }

}
