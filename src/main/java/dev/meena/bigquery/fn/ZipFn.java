package dev.meena.bigquery.fn;

import jakarta.ws.rs.BadRequestException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class ZipFn {

    public static String decodeRequestBody(String contentEncoding, byte[] requestBody) {
        if (requestBody == null || requestBody.length == 0)
            return "";

        boolean gzipEncoded = contentEncoding != null
                && contentEncoding.toLowerCase().contains("gzip");

        try {
            if (gzipEncoded || looksLikeGzip(requestBody)) {
                try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(requestBody))) {
                    return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            return new String(requestBody, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BadRequestException("Unable to decode request body", e);
        }
    }

    public static boolean looksLikeGzip(byte[] bytes) {
        return bytes.length >= 2
                && (bytes[0] & 0xff) == 0x1f
                && (bytes[1] & 0xff) == 0x8b;
    }
}
