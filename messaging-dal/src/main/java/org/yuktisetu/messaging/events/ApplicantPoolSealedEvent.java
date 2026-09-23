package org.yuktisetu.messaging.events;

import java.time.Instant;

/** The handover point to the shortlisting engine, which is not built yet. */
public record ApplicantPoolSealedEvent(
        Long postingId,
        Instant closedAt,
        int applicantCount,
        int eligibleCount,
        String shortlistingMode
) {}
