package org.yuktisetu.drive.model;

/** How one drive's scoring run ended. */
public enum ScoreRunStatus {

    /** Every applicant in the pool has a stored composite and rank. */
    SCORED,

    /**
     * Some applicants had a weighted factor that could not be measured, so nobody
     * was scored. The TnP approves proceeding, and the run is redone as SCORED.
     */
    HELD_FOR_REVIEW,

    /** The pool was too small for ranking to mean anything; shortlist manually. */
    DECLINED
}
