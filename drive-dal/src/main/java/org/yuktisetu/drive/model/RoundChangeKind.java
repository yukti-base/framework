package org.yuktisetu.drive.model;

/** Why a round result changed, in round_result_history. */
public enum RoundChangeKind {
    /** A draft mark set or changed before the round was published. */
    MARK,
    /** A draft mark removed before the round was published. */
    CLEAR,
    /** Left unmarked at publish, so recorded as not advanced. */
    PUBLISH_DEFAULT,
    /** A coordinator changed a published result, with a reason. */
    CORRECTION,
    /**
     * A draft mark in the next round removed because a correction took the
     * student out of it.
     */
    CASCADE_CLEARED
}
