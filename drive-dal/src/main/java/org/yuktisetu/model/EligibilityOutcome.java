package org.yuktisetu.model;

import java.util.List;

/**
 * The result of evaluating one student against one posting's gate.
 *
 * failureCodes is the answer to "why am I not seeing this drive", and
 * unevaluatedCodes is the answer to "which criteria could not be checked at
 * all" -- kept separate so an un-checkable criterion never masquerades as a
 * passed one. needsReview means a MissingDataPolicy of NEEDS_REVIEW was hit:
 * the student is not eligible automatically, but a human should look rather
 * than the system silently rejecting them over an un-synced ERP column.
 */
public record EligibilityOutcome(
        boolean eligible,
        boolean needsReview,
        List<String> failureCodes,
        List<String> unevaluatedCodes
) {
    public static EligibilityOutcome eligible(List<String> unevaluated) {
        return new EligibilityOutcome(true, false, List.of(), unevaluated);
    }

    public boolean hasFailures() {
        return !failureCodes.isEmpty();
    }
}
