package dev.meena.controllers.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class BigQueryResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String jobs() {
        Response response = new Response(null, "bigquery#jobsList", null, null, null);
        try {
            return OBJECT_MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{ \"error\": \"Unable to serialize response\" }";
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Response(
            @JsonProperty("etag")
            String etag,
            @JsonProperty("kind")
            String kind,
            @JsonProperty("nextPageToken")
            String nextPageToken,
            @JsonProperty("jobs")
            List<Job> jobs,
            @JsonProperty("jobReference")
            JobReference jobReference
    ) {
    }

    record Job(String id, String kind, String state) {
    }

    record JobReference(String projectId,
                        String jobId,
                        String location) {
    }


}
