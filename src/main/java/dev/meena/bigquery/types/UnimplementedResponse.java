package dev.meena.bigquery.types;

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
