package dev.meena.bigquery.jobs;

import dev.meena.bigquery.responses.QueryResponses;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.meena.bigquery.db.Postgres;
import dev.meena.bigquery.fn.Fn;
import dev.meena.bigquery.model.GetQueryResultsResponse;
import dev.meena.bigquery.model.Job;
import dev.meena.bigquery.model.TableReference;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class PgJobStorage {

    private static final Logger log = Logger.getLogger("PgJobStorage");

    public static final String JOBS_PROJECT = "bq";
    public static final String JOBS_DATASET = "jobs";
    public static final String JOBS_SCHEMA = "bq$jobs";
    public static final String JOBS_DETAILS_TABLE = "details";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    AgroalDataSource dataSource;

    @Inject
    Postgres postgres;

    private volatile boolean jobTableExists;

    public void put(String projectId, String jobId, Job jobDefinition) {
        put(projectId, jobId, jobDefinition, null);
    }

    public void put(String projectId, String jobId, Job jobDefinition, Object errorJson) {
        ensureJobsTableExists();
        TableReference destinationTable = destinationTable(jobDefinition);

        String location = jobDefinition.getJobReference() == null
                ? "US"
                : defaultString(jobDefinition.getJobReference().getLocation(), "US");

        String sql = """
                INSERT INTO "bq$jobs".details (
                    id,
                    project_id,
                    job_id,
                    location,
                    job_definition,
                    destination_project_id,
                    destination_dataset_id,
                    destination_table_id,
                    error,
                    completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (project_id, job_id)
                DO UPDATE SET
                    location = EXCLUDED.location,
                    job_definition = EXCLUDED.job_definition,
                    destination_project_id = EXCLUDED.destination_project_id,
                    destination_dataset_id = EXCLUDED.destination_dataset_id,
                    destination_table_id = EXCLUDED.destination_table_id,
                    error = EXCLUDED.error,
                    completed_at = EXCLUDED.completed_at
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setObject(1, UUID.randomUUID());
            stmt.setString(2, projectId);
            stmt.setString(3, jobId);
            stmt.setString(4, location);
            stmt.setObject(5, jsonb(jobDefinition));
            stmt.setString(6, defaultString(destinationTable.getProjectId(), projectId));
            stmt.setString(7, destinationTable.getDatasetId());
            stmt.setString(8, destinationTable.getTableId());

            if (errorJson == null) {
                stmt.setObject(9, null);
            } else {
                stmt.setObject(9, jsonb(errorJson));
            }

            stmt.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to store job " + projectId + ":" + jobId + ": " + e.getMessage(), e);
        }
    }

    public Job jobDefinition(String projectId, String jobId) {
        ensureJobsTableExists();

        String sql = """
                SELECT job_definition
                FROM %s.%s
                WHERE project_id = ?
                  AND job_id = ?
                LIMIT 1
                """.formatted(JOBS_SCHEMA, JOBS_DETAILS_TABLE);

        try (var con = dataSource.getConnection();
             var stmt = con.prepareStatement(sql)) {
            stmt.setString(1, projectId);
            stmt.setString(2, jobId);

            try (var rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return OBJECT_MAPPER.readValue(rs.getString("job_definition"), Job.class);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load job " + projectId + ":" + jobId + ": " + e.getMessage(), e);
        }
    }

    public GetQueryResultsResponse jobResults(String projectId, String jobId) {
        ensureJobsTableExists();
        JobDetails details = jobDetails(projectId, jobId);
        if (details == null || details.error() != null)
            return null;

        String schema = getSchema(details);
        String actualTable = postgres.resolveTableName(schema, details.destinationTableId());
        Postgres.QueryResult result;
        if (actualTable == null) {
            result = new Postgres.QueryResult(List.of(), List.of());
        } else {
            String sql = """
                    SELECT * FROM "%s"."%s"
                    """.formatted(schema, actualTable);
            result = postgres.query(sql);
            if (result == null)
                result = new Postgres.QueryResult(List.of(), List.of());
        }

        return QueryResponses.queryResultsResponse(
                details.projectId(),
                details.jobId(),
                details.location(),
                result
        );
    }

    public Postgres.QueryResult selectAll(TableReference destinationTable) {
        String schema = Fn.toSchema(destinationTable.getProjectId(), destinationTable.getDatasetId());
        String actualTable = postgres.resolveTableName(schema, destinationTable.getTableId());
        if (actualTable == null)
            return new Postgres.QueryResult(List.of(), List.of());

        String sql = """
                SELECT * FROM "%s"."%s"
                """.formatted(schema, actualTable);
        return postgres.query(sql);
    }

    public void delete(String projectId, String jobID) {
        JobDetails jobDetails = jobDetails(projectId, jobID);
        if (jobDetails == null)
            return;

        try (Connection con = dataSource.getConnection()) {
            String schema = getSchema(jobDetails);
            String actualTable = postgres.resolveTableName(schema, jobDetails.destinationTableId());
            String tableToDrop = actualTable != null ? actualTable : jobDetails.destinationTableId();
            con.prepareCall("""
                            DROP TABLE IF EXISTS "%s"."%s"
                            """.formatted(schema, tableToDrop))
                    .execute();

            String sql = """
                    DELETE FROM "%s".%s WHERE project_id = ? AND job_id = ?
                    """.formatted(JOBS_SCHEMA, JOBS_DETAILS_TABLE);
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, projectId);
            stmt.setString(2, jobID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to remove job " + projectId + ":" + jobID + ": " + e.getMessage(), e);
        }
    }

    public void materializeQueryResult(TableReference destinationTable,
                                       String translatedSql,
                                       Disposition writeDisposition,
                                       Disposition createDisposition) {
        if (isDdlStatement(translatedSql)) {
            postgres.execute(translatedSql);
            return;
        }

        String schema = Fn.toSchema(destinationTable.getProjectId(), destinationTable.getDatasetId());
        String existingTable = postgres.resolveTableName(schema, destinationTable.getTableId());
        boolean tableExists = existingTable != null;
        String table = tableExists ? existingTable : destinationTable.getTableId();
        String quotedTable = Fn.pgQuote(schema) + "." + Fn.pgQuote(table);

        boolean tableHasRows = tableExists && !postgres.isEmpty(quotedTable);

        if (Disposition.CREATE_NEVER == createDisposition && !tableExists)
            throw new IllegalStateException("Not found: Table " + quotedTable);

        else if (Disposition.CREATE_IF_NEEDED != createDisposition && Disposition.CREATE_NEVER != createDisposition)
            throw new IllegalArgumentException("Unsupported createDisposition: " + createDisposition);

        else if (Disposition.WRITE_EMPTY != writeDisposition
                && Disposition.WRITE_TRUNCATE != writeDisposition
                && Disposition.WRITE_APPEND != writeDisposition)
            throw new IllegalArgumentException("Unsupported writeDisposition: " + writeDisposition);

        postgres.execute("CREATE SCHEMA IF NOT EXISTS " + Fn.pgQuote(schema));

        switch (writeDisposition) {
            case Disposition.WRITE_TRUNCATE -> {
                postgres.execute("DROP TABLE IF EXISTS " + quotedTable);
                postgres.execute("CREATE TABLE " + quotedTable + " AS " + translatedSql);
                log.log(Level.INFO, "[PUT] " + "CREATE TABLE " + quotedTable + " AS " + translatedSql);
            }
            case Disposition.WRITE_APPEND -> {
                if (tableExists)
                    postgres.execute("INSERT INTO " + quotedTable + " " + translatedSql);
                else
                    postgres.execute("CREATE TABLE " + quotedTable + " AS " + translatedSql);
            }
            case Disposition.WRITE_EMPTY -> {
                if (tableHasRows)
                    throw new IllegalStateException("Already Exists: Table " + schema + "." + table);
                else if (tableExists)
                    postgres.execute("INSERT INTO " + quotedTable + " " + translatedSql);
                else
                    postgres.execute("CREATE TABLE " + quotedTable + " AS " + translatedSql);
            }
            default -> throw new IllegalArgumentException("Unsupported writeDisposition: " + writeDisposition);
        }
    }

    private JobDetails jobDetails(String projectId, String jobId) {
        String sql = """
                SELECT
                    project_id,
                    job_id,
                    location,
                    destination_project_id,
                    destination_dataset_id,
                    destination_table_id,
                    error
                FROM "bq$jobs".details
                WHERE project_id = ?
                  AND job_id = ?
                LIMIT 1
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, projectId);
            stmt.setString(2, jobId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next())
                    return null;

                return new JobDetails(
                        rs.getString("project_id"),
                        rs.getString("job_id"),
                        rs.getString("location"),
                        rs.getString("destination_project_id"),
                        rs.getString("destination_dataset_id"),
                        rs.getString("destination_table_id"),
                        rs.getObject("error")
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load job details, project=" + projectId + ", job=" + jobId + ": " + e.getMessage(), e);
        }
    }

    private static TableReference destinationTable(Job jobDefinition) {
        if (jobDefinition.getConfiguration() == null
                || jobDefinition.getConfiguration().getQuery() == null
                || jobDefinition.getConfiguration().getQuery().getDestinationTable() == null) {
            throw new IllegalArgumentException("Job destination table is required");
        }

        return jobDefinition.getConfiguration().getQuery().getDestinationTable();
    }

    // Initialize the job table if it doesn't exist yet
    private void ensureJobsTableExists() {
        if (jobTableExists)
            return;

        synchronized (this) {
            if (jobTableExists)
                return;

            postgres.execute("CREATE SCHEMA IF NOT EXISTS \"" + JOBS_SCHEMA + "\"");
            postgres.execute("""
                    CREATE TABLE IF NOT EXISTS "bq$jobs".details (
                        id UUID PRIMARY KEY,
                        project_id TEXT NOT NULL,
                        job_id TEXT NOT NULL,
                        location TEXT NOT NULL DEFAULT 'US',
                        job_definition JSONB NOT NULL,
                        destination_project_id TEXT NOT NULL,
                        destination_dataset_id TEXT NOT NULL,
                        destination_table_id TEXT NOT NULL,
                        error JSONB,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        completed_at TIMESTAMPTZ,
                        UNIQUE (project_id, job_id)
                    )
                    """);

            jobTableExists = true;
        }
    }

    private static PGobject jsonb(Object value) throws SQLException, JsonProcessingException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        if (value == null) {
            jsonb.setValue(null);
        } else if (value instanceof String str) {
            jsonb.setValue(str);
        } else {
            jsonb.setValue(OBJECT_MAPPER.writeValueAsString(value));
        }
        return jsonb;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static @Nullable String getSchema(JobDetails jobDetails) {
        return Fn.toSchema(jobDetails.destinationProjectId(), jobDetails.destinationDatasetId());
    }

    private static boolean isDdlStatement(String sql) {
        if (sql == null) return false;
        String normalized = sql.stripLeading().toLowerCase(Locale.ROOT);
        return normalized.startsWith("drop table ")
                || normalized.startsWith("create table ")
                || normalized.startsWith("update ")
                || normalized.startsWith("create or replace table ");
    }

    private record JobDetails(
            String projectId,
            String jobId,
            String location,
            String destinationProjectId,
            String destinationDatasetId,
            String destinationTableId,
            Object error
    ) {
    }
}