package org.yuktisetu.drive.model;

/**
 * Only APPLIED, WITHDRAWN, DISQUALIFIED, IN_REVIEW and DRIVE_CANCELLED are
 * reachable today. The rest are declared so the enum never needs a migration when
 * the shortlisting engine lands; ApplicationStatusTransitions rejects them until then.
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
