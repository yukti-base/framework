package org.yuktisetu.drive.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * The full derivation of one student's composite, returned by the scorer.
 *
 * Returned rather than logged, because CompositeScorer is a pure function and
 * must not hold a Logger -- coupling it to a logging framework would cost the
 * unit-testability that the EligibilityEvaluator precedent exists to protect.
 * The caller logs it, the tests assert on it, and the pipeline persists it: one
 * structure, three uses, which is what keeps the log and the stored breakdown
 * from ever disagreeing.
 *
 * Everything needed to answer "why is my score this?" is here, including the
 * sample each percentile was taken over -- without that, a percentile is an
 * unfalsifiable number.
 */
public record ScoringTrace(
        Long studentUserId,
        List<FactorTrace> factors,
        /** True when some factor was unmeasurable and the weights were rescaled. */
        boolean weightsRenormalised,
        BigDecimal composite,
        BigDecimal threshold,
        boolean meetsThreshold,
        /** Factors whose per-factor floor the student failed, which excludes them outright. */
        List<ScoringFactor> floorsFailed
) {
    /**
     * One factor's chain, in the order it is computed.
     *
     * raw -> percentile (over sampleSize) -> weight -> contribution
     */
    public record FactorTrace(
            ScoringFactor factor,
            FactorValue value,
            BigDecimal raw,
            BigDecimal percentile,
            int sampleSize,
            BigDecimal weightApplied,
            BigDecimal contribution,
            BigDecimal floor,
            boolean floorFailed,
            /** Set when the factor did not contribute, naming why. */
            String excludedReason
    ) {
        public boolean contributed() {
            return excludedReason == null;
        }
    }

    public boolean shortlistEligible() {
        return meetsThreshold && floorsFailed.isEmpty();
    }
}
