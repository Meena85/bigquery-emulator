package dev.meena.bigquery.types;

import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.jobs.Disposition;
import dev.meena.bigquery.model.*;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class JobBuilder {

    private static final JobStatus DONE = new JobStatus(null, "DONE");
    private static final String KIND = "bigquery#job";

    private final String projectId;
    private final String jobId;
    private String location;
    private String query;
    private TableReference destinationTable;
    private Postgres.QueryResult result;
    private long startTime;
    private String writeDisposition;
    private String createDisposition;
    private String errorMessage;

    public JobBuilder(String projectId, String jobId) {
        this.projectId = projectId;
        this.jobId = jobId;
        this.location = "US";
    }

    public JobBuilder location(String location) {
        if (isBlank(location)) return this;
        this.location = location;
        return this;
    }

    public JobBuilder query(String query) {
        this.query = query;
        return this;
    }

    public JobBuilder destinationTable(TableReference destinationTable) {
        this.destinationTable = destinationTable;
        return this;
    }

    public JobBuilder result(Postgres.QueryResult result) {
        this.result = result;
        return this;
    }

    public JobBuilder startTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public JobBuilder writeDisposition(Disposition writeDisposition) {
        this.writeDisposition = Disposition.toString(writeDisposition);
        return this;
    }

    public JobBuilder createDisposition(Disposition createDisposition) {
        this.createDisposition = Disposition.toString(createDisposition);
        return this;
    }

    public JobBuilder errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public Job build() {
        JobReference jobRef = new JobReference();
        jobRef.setProjectId(this.projectId);
        jobRef.setJobId(this.jobId);
        jobRef.setLocation(this.location);

        JobConfigurationQuery jobConfig = new JobConfigurationQuery();
        jobConfig.setQuery(this.query);
        jobConfig.setUseLegacySql(false);
        jobConfig.setDestinationTable(this.destinationTable);
        jobConfig.setCreateDisposition(this.createDisposition);
        jobConfig.setWriteDisposition(this.writeDisposition);

        JobConfiguration configuration = new JobConfiguration();
        configuration.setQuery(jobConfig);

        Job response = new Job("\"" + this.jobId + "\"",
                this.projectId + ":" + this.location + "." + this.jobId,
                KIND,
                null,
                "/bigquery/v2/projects/" + this.projectId + "/jobs/" + this.jobId,
                "db@bigquery.local");
        response.setConfiguration(configuration);
        response.setJobReference(jobRef);

        if (this.startTime != 0)
            response.setStatistics(jobStatistics(startTime, Instant.now().toEpochMilli(), result));

        if (isBlank(this.errorMessage))
            response.setStatus(DONE);
        else
            response.setStatus(errorJobStatus("invalidQuery", "query", this.errorMessage));

        return response;
    }

    private static JobStatistics jobStatistics(long startTime, long endTime, Postgres.QueryResult result) {
        JobStatistics stats = new JobStatistics(
                1.0,
                String.valueOf(startTime),
                String.valueOf(endTime),
                String.valueOf(endTime - startTime),
                "1",
                "parentId",
                List.of(),
                List.of(),
                null,
                String.valueOf(startTime),
                "1024",
                "2000");

        if (result != null)
            stats.setQuery(queryStats(result));

        return stats;
    }

    @SuppressWarnings("SameParameterValue")
    private static JobStatus errorJobStatus(String reason, String location, String message) {
        ErrorProto err = new ErrorProto();
        err.setReason(reason);
        err.setLocation(location);
        err.setMessage(message);

        List<ErrorProto> errors = new ArrayList<>();
        errors.add(err);

        return new JobStatus(errors, "DONE");
    }

    private static @NonNull JobStatistics2 queryStats(Postgres.QueryResult result) {
        JobStatistics2 queryStats = new JobStatistics2();
        queryStats.setSchema(Fn.tableSchema(result.columns()));
        return queryStats;
    }
}
