package org.yuktisetu.drive.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.yuktisetu.drive.model.ApplicationStatus.*;

/**
 * Legal application lifecycle moves.
 *
 * Only APPLIED, WITHDRAWN, DISQUALIFIED, IN_REVIEW and DRIVE_CANCELLED are
 * reachable today -- everything else past IN_REVIEW belongs to the shortlisting
 * engine, which is not built. The transitions are declared anyway so the enum never needs a
 * migration, and REACHABLE_NOW lets the service reject the rest explicitly
 * rather than by accident.
 *
 * WITHDRAWN -> APPLIED is intentional: re-applying before the deadline revives
 * the SAME row, which is what keeps uk_application_posting_student absolute.
 *
 * DRIVE_CANCELLED is reachable from every live status so that cancelling a drive
 * has somewhere to put its applications. Without it they stayed at APPLIED or
 * IN_REVIEW against a CANCELLED posting forever -- unreachable by any transition,
 * and shown to the student as still in progress.
 *
 * It is deliberately NOT reachable from OFFERED, OFFER_ACCEPTED or OFFER_DECLINED.
 * An offer that has already been made is a commitment to a named student, and
 * whether cancelling the drive revokes it is a decision for the TnP on the record,
 * never a side effect of a bulk UPDATE.
 */
public final class ApplicationStatusTransitions {

    private ApplicationStatusTransitions() {}

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = new EnumMap<>(ApplicationStatus.class);
    static {
        ALLOWED.put(APPLIED, EnumSet.of(WITHDRAWN, DISQUALIFIED, IN_REVIEW, DRIVE_CANCELLED));
        ALLOWED.put(WITHDRAWN, EnumSet.of(APPLIED));
        // -> APPLIED from the three pool statuses is the TnP reopening a sealed
        // drive (2026-10-27): the ranking and shortlist are discarded and every
        // application in the pool waits for the next seal. Nothing past
        // SHORTLISTED goes back -- a drive with offers cannot be reopened.
        ALLOWED.put(IN_REVIEW, EnumSet.of(SHORTLISTED, REJECTED, DISQUALIFIED, DRIVE_CANCELLED, APPLIED));
        ALLOWED.put(SHORTLISTED, EnumSet.of(OFFERED, REJECTED, DISQUALIFIED, DRIVE_CANCELLED, APPLIED));
        ALLOWED.put(OFFERED, EnumSet.of(OFFER_ACCEPTED, OFFER_DECLINED));
        ALLOWED.put(REJECTED, EnumSet.of(APPLIED));
        ALLOWED.put(DISQUALIFIED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(OFFER_ACCEPTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(OFFER_DECLINED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(DRIVE_CANCELLED, EnumSet.noneOf(ApplicationStatus.class));
    }

    /**
     * Statuses a live drive may still be moved into, used to keep the pool sweep
     * from touching rows that are already finished.
     */
    private static final Set<ApplicationStatus> CANCELLABLE =
            EnumSet.of(APPLIED, IN_REVIEW, SHORTLISTED);

    /** What this phase of the system may actually set. */
    private static final Set<ApplicationStatus> REACHABLE_NOW =
            EnumSet.of(APPLIED, WITHDRAWN, DISQUALIFIED, IN_REVIEW, DRIVE_CANCELLED);

    public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean isReachableNow(ApplicationStatus status) {
        return REACHABLE_NOW.contains(status);
    }

    /**
     * The statuses that cancelling a drive sweeps into DRIVE_CANCELLED.
     *
     * Exposed as a set rather than hardcoded in the service so that the bulk
     * UPDATE and canTransition() can never disagree about which rows are fair
     * game -- the mismatch would be silent and would strand rows exactly the way
     * this status was added to prevent.
     */
    public static Set<ApplicationStatus> cancellableStatuses() {
        return CANCELLABLE;
    }
}
