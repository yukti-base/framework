package org.yuktisetu.drive.model;

/**
 * OFFER_ACCEPTED and OFFER_DECLINED are declared but not reachable yet; see
 * ApplicationStatusTransitions.
 *
 * During a drive's rounds a student stays SHORTLISTED while they keep
 * advancing; publishing a round makes everyone who did not advance REJECTED,
 * and clearing the final round makes them OFFERED. Which round they are in is
 * read from round_results, never stored here.
 */
public enum ApplicationStatus {
    APPLIED,
    WITHDRAWN,
    DISQUALIFIED,
    IN_REVIEW,
    SHORTLISTED,
    REJECTED,
    OFFERED,
    OFFER_ACCEPTED,
    OFFER_DECLINED,

    /**
     * The drive was cancelled under the student, so their application died with
     * it. Distinct from REJECTED and DISQUALIFIED on purpose: neither of those is
     * true, and a student who screenshots "Rejected" for a drive that never ran
     * has a grievance the TnP cannot answer. Terminal.
     */
    DRIVE_CANCELLED
}
