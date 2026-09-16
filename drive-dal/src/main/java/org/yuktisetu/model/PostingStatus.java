package org.yuktisetu.model;

/**
 * APPLICATIONS_CLOSED is the sealed applicant pool -- the handover point to the
 * future shortlisting engine, which moves a posting on to IN_PROGRESS.
 */
public enum PostingStatus {
    DRAFT,
    PENDING_APPROVAL,
    PUBLISHED,
    APPLICATIONS_CLOSED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
