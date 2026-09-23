package org.yuktisetu.drive.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.yuktisetu.drive.model.ApplicationStatus.*;

/**
 * Legal application lifecycle moves.
 *
 * Only APPLIED, WITHDRAWN, DISQUALIFIED and IN_REVIEW are reachable today --
 * everything past IN_REVIEW belongs to the shortlisting engine, which is not
 * built. The transitions are declared anyway so the enum never needs a
 * migration, and REACHABLE_NOW lets the service reject the rest explicitly
 * rather than by accident.
 *
 * WITHDRAWN -> APPLIED is intentional: re-applying before the deadline revives
 * the SAME row, which is what keeps uk_application_posting_student absolute.
 */
public final class ApplicationStatusTransitions {

    private ApplicationStatusTransitions() {}

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = new EnumMap<>(ApplicationStatus.class);
    static {
        ALLOWED.put(APPLIED, EnumSet.of(WITHDRAWN, DISQUALIFIED, IN_REVIEW));
        ALLOWED.put(WITHDRAWN, EnumSet.of(APPLIED));
        ALLOWED.put(IN_REVIEW, EnumSet.of(SHORTLISTED, REJECTED, DISQUALIFIED));
        ALLOWED.put(SHORTLISTED, EnumSet.of(OFFERED, REJECTED, DISQUALIFIED));
        ALLOWED.put(OFFERED, EnumSet.of(OFFER_ACCEPTED, OFFER_DECLINED));
        ALLOWED.put(REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(DISQUALIFIED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(OFFER_ACCEPTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(OFFER_DECLINED, EnumSet.noneOf(ApplicationStatus.class));
    }

    /** What this phase of the system may actually set. */
    private static final Set<ApplicationStatus> REACHABLE_NOW =
            EnumSet.of(APPLIED, WITHDRAWN, DISQUALIFIED, IN_REVIEW);

    public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean isReachableNow(ApplicationStatus status) {
        return REACHABLE_NOW.contains(status);
    }
}
