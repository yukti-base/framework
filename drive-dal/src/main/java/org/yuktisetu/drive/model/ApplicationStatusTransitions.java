package org.yuktisetu.drive.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.yuktisetu.drive.model.ApplicationStatus.*;

/**
 * Legal application lifecycle moves.
 *
 * OFFER_ACCEPTED and OFFER_DECLINED are not reachable yet: the student's answer
 * to an offer is not recorded in the app. Everything else is -- SHORTLISTED and
 * REJECTED by the shortlist and the drive's rounds, OFFERED by clearing the
 * final round. REACHABLE_NOW lets a service reject the rest explicitly rather
 * than by accident.
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
        // -> REJECTED from OFFERED is a coordinator correcting the final round
        // (2026-10-30): the student was marked advanced by mistake. The offer
        // record is revoked in the same transaction.
        ALLOWED.put(OFFERED, EnumSet.of(OFFER_ACCEPTED, OFFER_DECLINED, REJECTED));
        // -> SHORTLISTED / OFFERED from REJECTED is the same kind of correction
        // the other way: wrongly marked not advanced or absent in a published
        // round, put back in the drive (or offered, if it was the final round).
        ALLOWED.put(REJECTED, EnumSet.of(APPLIED, SHORTLISTED, OFFERED));
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
            EnumSet.of(APPLIED, WITHDRAWN, DISQUALIFIED, IN_REVIEW, DRIVE_CANCELLED,
                    SHORTLISTED, REJECTED, OFFERED);

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
