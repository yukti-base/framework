package org.yuktisetu.drive.model;

/**
 * What the TnP decided for an application whose coding data could not be
 * captured. Neither sends the student to the scoring engine -- by decision, the
 * engine never scores a student with unavailable data.
 */
public enum CodingReviewDecision {

    /**
     * On the final list as a TnP pick, never scored. Uses one of the drive's
     * seats: the engine fills the target minus the direct picks.
     */
    SHORTLIST_DIRECTLY,

    /** Out of this drive's shortlisting. */
    REMOVE
}
