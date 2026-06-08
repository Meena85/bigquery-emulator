package dev.meena.bigquery.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.meena.bigquery.types.inner.*;

import java.util.List;

public abstract class Response {

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

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public JobReference getJobReference() {
        return jobReference;
    }

    public void setJobReference(JobReference jobReference) {
        this.jobReference = jobReference;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
