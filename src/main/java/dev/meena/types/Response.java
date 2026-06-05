package dev.meena.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.meena.types.inner.*;

import java.util.List;

public abstract class Response {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("etag")
    String etag;

    @JsonProperty("kind")
    String kind;

    @JsonProperty("id")
    String id;

    @JsonProperty("nextPageToken")
    String nextPageToken;

    @JsonProperty("selfLink")
    String selfLink;

    @JsonProperty("jobs")
    List<Job> jobs;

    @JsonProperty("jobReference")
    JobReference jobReference;

    @JsonProperty("statistics")
    Statistics statistics;

    @JsonProperty("status")
    Status status;

    @JsonProperty("configuration")
    Configuration configuration;
}
