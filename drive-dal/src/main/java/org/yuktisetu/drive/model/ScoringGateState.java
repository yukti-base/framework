package org.yuktisetu.drive.model;

/**
 * Whether a sealed COMPOSITE_SCORE drive may be scored yet.
 *
 * Moves forward only, and is moved by whichever event settles the last open
 * condition -- the deadline sealing the pool, the last scrape finishing, or the
 * TnP resolving the last flagged student. Nothing waits on it or polls it to
 * make progress.
 *
 *   COLLECTING ──(every capture settled, none flagged)──────────────► READY
 *        │                                                              ▲
 *        └──(every capture settled, some flagged)──► AWAITING_TNP_REVIEW ┘
 *                                                     (TnP resolves the last one)
 */
public enum ScoringGateState {

    /** Some capture in the sealed pool is still being scraped. */
    COLLECTING,

    /** Every capture has settled, and the TnP has students to review. */
    AWAITING_TNP_REVIEW,

    /** DriveReadyForScoring has been written. Terminal. */
    READY,

    /** The drive was cancelled before it became ready. Terminal. */
    CANCELLED;

    public boolean isOpen() {
        return this == COLLECTING || this == AWAITING_TNP_REVIEW;
    }
}
