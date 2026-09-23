package org.yuktisetu.profile.model;

/** Why a document URL was handed out -- the "what was given, and for what" half of the log. */
public enum DocumentAccessReason {
    /** The student listed or opened their own document. */
    OWNER_VIEW,
    /** A TnP role opened a single student's document. */
    ADMIN_VIEW,
    /** A resume was attached to a job application. */
    APPLICATION_ATTACHED,
    /** URLs were emitted into an applicant list the admin downloaded. */
    APPLICANT_EXPORT
}
