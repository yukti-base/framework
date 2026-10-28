package org.yuktisetu.drive.model;

/** Whether a capture needs a human before the drive can be scored. */
public enum CaptureReviewState {

    /** Captured cleanly; nothing for the TnP to look at. */
    NOT_REQUIRED,

    /**
     * Something went wrong scraping this student. The drive's scoring gate stays
     * shut until the TnP resolves it -- the TnP, not the pipeline, decides
     * whether this student is scored on older data or without coding at all.
     */
    NEEDS_REVIEW,

    /** The TnP has decided. */
    RESOLVED
}
