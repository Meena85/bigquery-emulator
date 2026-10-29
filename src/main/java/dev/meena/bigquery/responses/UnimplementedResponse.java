package dev.meena.bigquery.responses;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record UnimplementedResponse(
        String message,
        String url,
        String path,
        String queryParameters,
        String requestBody
) {
    @Override
    public String toString() {
        return message + '\'' +
                ", url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", queryParameters='" + queryParameters + '\'' +
                ", requestBody='" + requestBody + '\'' +
                '}';
    }
}
