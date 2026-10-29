package dev.meena.bigquery.responses;

import dev.meena.bigquery.model.Job;
import dev.meena.bigquery.model.JobCancelResponse;
import dev.meena.bigquery.model.JobList;
import dev.meena.bigquery.model.JobListJobsInner;

import java.util.List;

/** Builds job list and cancel payloads from stored jobs. */
public final class JobResponses {

    private JobResponses() {
    }

    public static JobList jobList(String projectId, List<Job> jobs, String nextPageToken) {
        return new JobList()
                .kind("bigquery#jobList")
                .etag("\"" + projectId + ":jobs\"")
                .jobs(jobs.stream().map(JobResponses::listItem).toList())
                .nextPageToken(nextPageToken);
    }

    /** Jobs finish before they are returned, so cancelling reports the finished job. */
    public static JobCancelResponse cancel(Job job) {
        return new JobCancelResponse().kind("bigquery#jobCancelResponse").job(job);
    }

    private static JobListJobsInner listItem(Job job) {
        String state = job.getStatus() == null ? null : job.getStatus().getState();
        return new JobListJobsInner()
                ._configuration(job.getConfiguration())
                .id(job.getId())
                .kind("bigquery#job")
                .jobReference(job.getJobReference())
                .state(state)
                .status(job.getStatus())
                .errorResult(job.getStatus() == null ? null : job.getStatus().getErrorResult())
                .statistics(job.getStatistics())
                .userEmail(job.getUserEmail());
    }
}
