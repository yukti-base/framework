package org.yuktisetu.drive.model;

/**
 * Where one application's coding capture on one platform stands.
 *
 * PENDING is the only state the scoring gate waits on, and it cannot last: the
 * scrape job behind it has an absolute time limit (give_up_at), after which it
 * FAILS and the capture settles with it. So "wait until every capture settles"
 * is a wait with a known end.
 */
public enum CaptureState {

    /** Queued at apply, or re-checked by the student; its scrape job has not finished. */
    PENDING,

    /**
     * The student's handle could not be read (not found, or listed with no
     * username) while the deadline is still open. The student has been told and
     * may re-check it once. Ends at the latest when the deadline seals the pool:
     * a capture still here then is flagged NO_RECHECK. So, like PENDING, it is a
     * wait with a known end -- and before the seal no gate is waiting on it.
     */
    AWAITING_STUDENT,

    /** The scrape succeeded. snapshotId is the exact row this application is scored on. */
    CAPTURED,

    /**
     * Nothing usable was captured -- the handle was blank or not found, the
     * platform stayed down past the job's limit, or our parser broke.
     *
     * Paired with NEEDS_REVIEW (and a flagReason) when the TnP must decide, or
     * with NOT_REQUIRED when the student's re-check also failed on the handle and
     * they are scored on their other platforms instead.
     */
    FAILED,

    /**
     * Replaced by a newer capture for the same application, after a withdraw and
     * re-apply. Kept for the record and ignored by everything else.
     */
    SUPERSEDED
}
