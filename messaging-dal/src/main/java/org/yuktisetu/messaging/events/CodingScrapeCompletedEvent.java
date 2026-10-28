package org.yuktisetu.messaging.events;

import java.time.Instant;

/**
 * A scrape job reached its end. Produced by yuktisetu-scraper (Python), which
 * writes it into the shared outbox in the same transaction that finishes the
 * job; drive-service's relay publishes it and drive-service consumes it.
 *
 * A pure fact about a scrape. It carries nothing about drives or applications --
 * the scraper does not know what a drive is, and drive-service decides what the
 * result means for each application waiting on this job.
 *
 * jobStatus is DONE or FAILED. outcome is the scraper's verdict: OK,
 * HANDLE_NOT_FOUND, PLATFORM_ERROR, PARSE_FAILED or RATE_LIMITED. snapshotId is
 * set only on DONE.
 */
public record CodingScrapeCompletedEvent(
        Long jobId,
        Long studentUserId,
        String platform,
        String handle,
        String jobStatus,
        String outcome,
        Long snapshotId,
        Integer attempts,
        String error,
        Instant finishedAt
) {
    public boolean captured() {
        return "DONE".equals(jobStatus) && snapshotId != null;
    }
}
