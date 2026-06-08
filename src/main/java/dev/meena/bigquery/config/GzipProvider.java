package dev.meena.bigquery.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

@Provider
public class GzipProvider implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Check if the Content-Encoding header specifies gzip
        String contentEncoding = requestContext.getHeaderString(HttpHeaders.CONTENT_ENCODING);

        if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
            InputStream originalStream = requestContext.getEntityStream();

            // Wrap the original stream in a GZIPInputStream
            requestContext.setEntityStream(new GZIPInputStream(originalStream));

            // Crucial: Remove or modify the header so downstream body readers (Jackson)
            // know the stream hitting them is now raw uncompressed JSON.
            requestContext.getHeaders().remove(HttpHeaders.CONTENT_ENCODING);
        }
    }
}
