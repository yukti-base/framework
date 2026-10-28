package org.yuktisetu.drive.model;

import java.math.BigDecimal;

/**
 * One factor's input for one student -- and, crucially, WHY it is what it is.
 *
 * This three-way split is the single most important decision in the scoring
 * design, and a plain nullable number cannot express it. There is no missing-data
 * policy that is correct in general; the right answer depends entirely on whether
 * the data is absent because we could not measure it, or absent because the
 * honest measurement is "none".
 *
 * Worked through with real numbers, on a drive weighting Certifications at 25%
 * with two students otherwise identical at percentile 60:
 *
 *   Student A has no certifications. Student B has one weak one, at percentile 10.
 *   Treat A's absence as UNMEASURABLE and renormalise, and A scores 60.00 while B
 *   scores 47.50 -- having nothing beats having something weak by 12.5 points.
 *   That rewards not trying, and it is the outcome a nullable column produces by
 *   default.
 *
 *   Now a platform is down at the deadline. Treat that as a zero and the affected
 *   student loses 16.5 points for an outage they did not cause.
 *
 * So the two cases need opposite handling, and the model has to carry the
 * difference rather than leaving the scorer to guess.
 */
public sealed interface FactorValue {

    /** We measured it and this is the number. */
    record Measured(BigDecimal raw) implements FactorValue {
        public Measured {
            if (raw == null) {
                throw new IllegalArgumentException("Measured requires a value; use MeasuredNone");
            }
        }
    }

    /**
     * We measured it and the answer is genuinely nothing: no certifications, no
     * work experience, no skills matched.
     *
     * Scored at the BOTTOM of the pool's distribution for that factor, not
     * excluded -- because it is a real and comparable fact about the student, and
     * excluding it would rank them above a peer who has something modest.
     */
    record MeasuredNone() implements FactorValue {}

    /**
     * We could not measure it, through no fault of the student: the coding
     * platform was unreachable, the factor was never configured for this drive.
     *
     * The factor is dropped for this student and their remaining weights are
     * rescaled to 100%. Available-case analysis, which is unbiased when the
     * missingness is unrelated to ability -- true for an outage, and the reason
     * this must never be used for a genuine absence, where missingness is
     * extremely informative.
     *
     * The reason is carried so a student asking why their score moved gets an
     * answer, and so the stale-data hold can name the platform to the TnP.
     */
    record Unmeasurable(String reason) implements FactorValue {}

    static FactorValue of(BigDecimal raw) {
        return raw == null ? new MeasuredNone() : new Measured(raw);
    }

    default boolean contributes() {
        return !(this instanceof Unmeasurable);
    }
}
