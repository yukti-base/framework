package org.yuktisetu.drive.model;

/**
 * Why a capture was flagged for the TnP. Shown on the review screen and in the
 * student's notice, so both read the same reason.
 */
public enum CaptureFlagReason {

    /**
     * Not the student's fault: the platform stayed down or blocked us past the
     * job's limit, or our parser broke. An outage hits every student on that
     * platform at once, so it is never quietly skipped.
     */
    SERVER_ERROR,

    /** The student was asked to re-check their handle and did not before the deadline. */
    NO_RECHECK,

    /** The handle failed after the deadline had passed, so there was no time to re-check. */
    AFTER_DEADLINE,

    /** Every platform the student listed failed, so there is nothing to score them on. */
    NOTHING_LEFT
}
