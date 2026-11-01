package org.yuktisetu.drive.model;

import java.util.Optional;

/**
 * What a round result does to a student's application, in one place.
 *
 * Shared by publishing a round and correcting one, so the two can never disagree
 * about what "advanced in the final round" means. Spring-free, like the other
 * transition tables, so it is pinned by plain unit tests.
 *
 * The invariant every rule here keeps: while a drive's rounds run, the students
 * still SHORTLISTED are exactly the candidates of the drive's current round.
 * Publishing a round takes everyone who did not advance out (REJECTED); clearing
 * the final round makes the student OFFERED; a correction moves one student
 * across the same line, in either direction.
 */
public final class RoundRules {

    private RoundRules() {}

    /** The status a candidate (currently SHORTLISTED) takes when their round is published. */
    public static ApplicationStatus statusAfterPublish(RoundOutcome outcome, boolean finalRound) {
        if (outcome.advances()) {
            return finalRound ? ApplicationStatus.OFFERED : ApplicationStatus.SHORTLISTED;
        }
        return ApplicationStatus.REJECTED;
    }

    /**
     * The application move a correction of a published result causes, if any.
     *
     * NOT_ADVANCED <-> ABSENT changes what the student is told but not where they
     * stand: both are out of the drive, so the application does not move.
     */
    public static Optional<StatusMove> moveForCorrection(RoundOutcome from, RoundOutcome to, boolean finalRound) {
        if (from == to || from.advances() == to.advances()) {
            return Optional.empty();
        }
        ApplicationStatus in = finalRound ? ApplicationStatus.OFFERED : ApplicationStatus.SHORTLISTED;
        return Optional.of(to.advances()
                ? new StatusMove(ApplicationStatus.REJECTED, in)
                : new StatusMove(in, ApplicationStatus.REJECTED));
    }

    /** One application status change: the status it must be in, and the one it moves to. */
    public record StatusMove(ApplicationStatus from, ApplicationStatus to) {
        public StatusMove {
            if (!ApplicationStatusTransitions.canTransition(from, to)) {
                throw new IllegalStateException("Round rule produced an illegal move " + from + " -> " + to);
            }
        }
    }
}
