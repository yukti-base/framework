package org.yuktisetu.drive.model;

/**
 * Only APPLIED, WITHDRAWN, DISQUALIFIED and IN_REVIEW are reachable today.
 * The rest are declared so the enum never needs a migration when the
 * shortlisting engine lands; ApplicationStatusTransitions rejects them until then.
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
    OFFER_DECLINED
}
