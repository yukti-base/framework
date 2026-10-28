package org.yuktisetu.messaging.events;

import java.time.Instant;

/**
 * Every condition for scoring this drive is met. The scoring engine's trigger.
 *
 * Written exactly once per drive, in the transaction that settled the last
 * condition: the pool is sealed, every applicant's coding capture has finished,
 * and the TnP has resolved every flagged student. Independent of how it got
 * there -- the engine needs to know nothing about scraping, retries or reviews,
 * only that the pool is ready and how it is made up.
 *
 * What the engine must do with the TnP's decisions (application_coding_reviews):
 *   - score NEITHER kind -- a reviewed student is never scored;
 *   - SHORTLIST_DIRECTLY students are already on the final list;
 *   - fill engineSeats, which is the shortlist target minus those direct picks
 *     (null when the drive set no target).
 */
public record DriveReadyForScoringEvent(
        Long postingId,
        Instant readyAt,
        int poolSize,
        int capturedCount,
        int failedCount,
        String shortlistingMode,
        Integer shortlistTarget,
        int directlyShortlisted,
        int removedByReview,
        Integer engineSeats
) {}
