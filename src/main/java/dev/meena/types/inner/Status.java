package dev.meena.types.inner;

import java.util.List;

public record Status(
        String state,
        Error errorResult,
        List<Error> errors
) {

    record Error(String message, String reason, String location) {
    }
}
