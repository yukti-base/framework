package org.yuktisetu.drive.model;

/**
 * One candidate's result in one round.
 *
 * NOT_ADVANCED and ABSENT have the same consequence -- the student is out of the
 * drive -- but are kept apart because they are different facts, and the student
 * and their department are told which one it was (decided 2026-10-30).
 */
public enum RoundOutcome {
    ADVANCED("Advanced"),
    NOT_ADVANCED("Not advanced"),
    ABSENT("Absent");

    private final String label;

    RoundOutcome(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean advances() {
        return this == ADVANCED;
    }
}
