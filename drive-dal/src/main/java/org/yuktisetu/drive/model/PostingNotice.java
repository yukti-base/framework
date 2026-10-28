package org.yuktisetu.drive.model;

import java.time.Instant;

/**
 * Everything a notification needs to say about a drive, as plain values.
 *
 * Exists so the notification code never touches a JobPosting entity. Reading
 * posting.getCompany().getName() off a managed entity is a lazy load, and it
 * throws LazyInitializationException the moment that entity has been detached --
 * which a @Modifying(clearAutomatically = true) query, a finished transaction or
 * a hop onto another thread all do. The failure was silent in the worst way: the
 * notification was swallowed by a catch-all and no student was ever told.
 *
 * Built by a JPQL projection straight from the database by id, so it is always
 * current and can never be stale, detached or half-loaded.
 */
public record PostingNotice(
        Long id,
        String title,
        String companyName,
        Instant applicationDeadline,
        Long publishedBy,
        boolean notifyOnPublish
) {}
