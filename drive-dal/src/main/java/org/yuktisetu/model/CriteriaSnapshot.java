package org.yuktisetu.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One posting's academic eligibility gate, flattened from
 * PostingEligibilityCriteria and its child rows.
 *
 * Exists so EligibilityEvaluator never touches an entity: admin-service can
 * build one for a pre-publish preview and drive-service can build one for
 * enforcement, and both get byte-identical behaviour. A snapshot is also the
 * right shape to persist later, when the scoring engine needs to record exactly
 * what gate a decision was made against.
 *
 * Each gate is a flag plus a threshold, mirroring the entity: a disabled gate is
 * skipped entirely, never evaluated as "threshold zero".
 */
public record CriteriaSnapshot(
        boolean aggregateCgpaEnabled, BigDecimal minAggregateCgpa,
        boolean semesterCgpaEnabled, Map<Integer, BigDecimal> semesterMinGpa,
        boolean tenthEnabled, BigDecimal minTenthPercentage,
        boolean twelfthEnabled, BigDecimal minTwelfthPercentage,
        boolean diplomaEnabled, BigDecimal minDiplomaPercentage,
        AlternativeGateMode twelfthOrDiplomaMode,
        boolean activeBacklogsEnabled, Integer maxActiveBacklogs,
        boolean totalBacklogsEnabled, Integer maxTotalBacklogs,
        boolean gapYearsEnabled, Integer maxGapYears,
        boolean cocubesEnabled, BigDecimal minCocubesScore,

        /** Exams other than CoCubes -- configurable but not yet evaluable. */
        List<AptitudeExam> unevaluableAptitudeExams,

        List<CustomCriterion> customCriteria,

        MissingDataPolicy missingDataPolicy,

        /** Whether the TnP's non-negotiable-field gate applies (SSOT 5.9). */
        boolean requireCompleteProfile
) {
    public record CustomCriterion(
            String criterionKey,
            String label,
            CriterionSource source,
            CriterionOperator operator,
            CriterionValueType valueType,
            String valueJson,
            boolean mandatory,
            MissingDataPolicy missingDataPolicy,
            /** False when nothing can resolve this criterion's student-side value. */
            boolean evaluable
    ) {}
}
