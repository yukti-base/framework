package org.yuktisetu.drive.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The academic half of the eligibility gate, as a pure function.
 *
 * Lives in drive-dal rather than in a service for the same reason RoleHierarchy
 * lives in user-dal: two services need the identical rule. admin-service runs it
 * for the pre-publish preview ("how many students would this reach?") and
 * drive-service runs it for enforcement. If they ever disagreed, the TnP would
 * publish a drive expecting 300 applicants and get 40, with no way to explain
 * the difference.
 *
 * Three invariants worth stating, because each one is a bug this class exists to
 * prevent:
 *
 *  1. A DISABLED gate is skipped, never evaluated against zero. Without the
 *     enable flags, an un-set criterion would silently admit everyone while the
 *     TnP believed it was filtering.
 *  2. A NULL student value on an ENABLED gate is decided by MissingDataPolicy,
 *     never guessed. Pass-on-null lets an incomplete profile clear every gate;
 *     fail-on-null lets one un-synced ERP column quietly remove a student from
 *     every drive in the season. Both are wrong as a default, so it is a choice.
 *  3. A criterion that CANNOT be evaluated is reported as unevaluated, not
 *     passed. A gate that quietly waves everyone through is worse than no gate.
 *
 * The re-application policy (placed / interning / uplift) is NOT here -- it
 * needs the student's offer history, so it lives in drive-service's
 * ReapplicationGuard and runs after this.
 */
public final class EligibilityEvaluator {

    private EligibilityEvaluator() {}

    public static EligibilityOutcome evaluate(CriteriaSnapshot criteria, CandidateFacts facts) {
        List<String> failures = new ArrayList<>();
        List<String> unevaluated = new ArrayList<>();
        boolean needsReview = false;

        // SSOT 5.9: a student who has not completed the TnP's non-negotiable
        // fields is ineligible for EVERY drive, not only ones checking that field.
        if (criteria.requireCompleteProfile() && !facts.profileComplete()) {
            failures.add(EligibilityFailureCode.PROFILE_INCOMPLETE);
        }

        // --- Aggregate CGPA ---
        if (criteria.aggregateCgpaEnabled()) {
            needsReview |= checkMin(facts.aggregateCgpa(), criteria.minAggregateCgpa(),
                    "cgpa", EligibilityFailureCode.CGPA_BELOW_MIN,
                    criteria.missingDataPolicy(), failures);
        }

        // --- Semester-wise GPA ---
        if (criteria.semesterCgpaEnabled()) {
            for (Map.Entry<Integer, BigDecimal> gate : criteria.semesterMinGpa().entrySet()) {
                BigDecimal actual = facts.semesterGpa() == null ? null : facts.semesterGpa().get(gate.getKey());
                needsReview |= checkMin(actual, gate.getValue(),
                        "sem" + gate.getKey() + "Gpa",
                        EligibilityFailureCode.SEMESTER_GPA_BELOW_MIN,
                        criteria.missingDataPolicy(), failures);
            }
        }

        // --- 10th ---
        if (criteria.tenthEnabled()) {
            needsReview |= checkMin(facts.tenthPercentage(), criteria.minTenthPercentage(),
                    "tenthPercentage", EligibilityFailureCode.TENTH_BELOW_MIN,
                    criteria.missingDataPolicy(), failures);
        }

        // --- 12th / diploma, combined per twelfthOrDiplomaMode ---
        needsReview |= checkTwelfthOrDiploma(criteria, facts, failures);

        // --- Backlogs ---
        if (criteria.activeBacklogsEnabled()) {
            needsReview |= checkMax(facts.activeBacklogs(), criteria.maxActiveBacklogs(),
                    "activeBacklogs", EligibilityFailureCode.ACTIVE_BACKLOGS_EXCEEDED,
                    criteria.missingDataPolicy(), failures);
        }
        if (criteria.totalBacklogsEnabled()) {
            needsReview |= checkMax(facts.totalBacklogs(), criteria.maxTotalBacklogs(),
                    "totalBacklogs", EligibilityFailureCode.TOTAL_BACKLOGS_EXCEEDED,
                    criteria.missingDataPolicy(), failures);
        }

        // --- Gap years ---
        if (criteria.gapYearsEnabled()) {
            needsReview |= checkMax(facts.gapYears(), criteria.maxGapYears(),
                    "gapYears", EligibilityFailureCode.GAP_YEARS_EXCEEDED,
                    criteria.missingDataPolicy(), failures);
        }

        // --- CoCubes: the only aptitude exam with a student-side column today ---
        if (criteria.cocubesEnabled()) {
            needsReview |= checkMin(facts.cocubesScore(), criteria.minCocubesScore(),
                    "cocubesScore", EligibilityFailureCode.COCUBES_BELOW_MIN,
                    criteria.missingDataPolicy(), failures);
        }

        // --- Aptitude exams with nowhere to read a score from ---
        if (criteria.unevaluableAptitudeExams() != null) {
            criteria.unevaluableAptitudeExams()
                    .forEach(exam -> unevaluated.add(EligibilityFailureCode.unevaluated(exam.name())));
        }

        // --- Company one-offs ---
        if (criteria.customCriteria() != null) {
            for (CriteriaSnapshot.CustomCriterion c : criteria.customCriteria()) {
                if (!c.evaluable()) {
                    unevaluated.add(EligibilityFailureCode.unevaluated(c.criterionKey()));
                    continue;
                }
                String actual = facts.customValues() == null ? null : facts.customValues().get(c.criterionKey());
                if (actual == null) {
                    switch (c.missingDataPolicy()) {
                        case INELIGIBLE -> {
                            if (c.mandatory()) {
                                failures.add(EligibilityFailureCode.missingData(c.criterionKey()));
                            }
                        }
                        case NEEDS_REVIEW -> needsReview = true;
                        case ELIGIBLE -> { /* explicitly waved through */ }
                    }
                    continue;
                }
                if (!CriterionMatcher.matches(c, actual) && c.mandatory()) {
                    failures.add(EligibilityFailureCode.custom(c.criterionKey()));
                }
            }
        }

        if (!failures.isEmpty()) {
            return new EligibilityOutcome(false, needsReview, List.copyOf(failures), List.copyOf(unevaluated));
        }
        // needsReview alone does not make a student eligible -- a human decides.
        return new EligibilityOutcome(!needsReview, needsReview, List.of(), List.copyOf(unevaluated));
    }

    /**
     * 12th and diploma together.
     *
     * AlternativeGateMode has exactly one value, EITHER_SUFFICIENT, so clearing
     * either route is enough whenever both gates are on. That is deliberate:
     * diploma-lateral students have no 12th marks at all, and requiring both
     * would silently exclude that entire cohort -- the kind of failure nobody
     * notices until a whole branch is missing from a drive. The mode is still
     * carried as an enum so a future policy can add a stricter combination
     * without reshaping the criteria table.
     */
    private static boolean checkTwelfthOrDiploma(CriteriaSnapshot criteria, CandidateFacts facts,
                                                 List<String> failures) {
        boolean twelfthOn = criteria.twelfthEnabled();
        boolean diplomaOn = criteria.diplomaEnabled();
        if (!twelfthOn && !diplomaOn) {
            return false;
        }

        if (twelfthOn && diplomaOn) {
            boolean twelfthOk = meetsMin(facts.twelfthPercentage(), criteria.minTwelfthPercentage());
            boolean diplomaOk = meetsMin(facts.diplomaPercentage(), criteria.minDiplomaPercentage());
            if (twelfthOk || diplomaOk) {
                return false;
            }
            // Neither route cleared. If the student has no value on either, this
            // is missing data rather than a failed gate.
            if (facts.twelfthPercentage() == null && facts.diplomaPercentage() == null) {
                return applyMissingPolicy(criteria.missingDataPolicy(), "twelfthOrDiploma", failures);
            }
            failures.add(EligibilityFailureCode.TWELFTH_OR_DIPLOMA_BELOW_MIN);
            return false;
        }

        // Exactly one of the two gates is enabled here.
        boolean needsReview = false;
        if (twelfthOn) {
            needsReview |= checkMin(facts.twelfthPercentage(), criteria.minTwelfthPercentage(),
                    "twelfthPercentage", EligibilityFailureCode.TWELFTH_BELOW_MIN,
                    criteria.missingDataPolicy(), failures);
        }
        if (diplomaOn) {
            needsReview |= checkMin(facts.diplomaPercentage(), criteria.minDiplomaPercentage(),
                    "diplomaPercentage", EligibilityFailureCode.DIPLOMA_BELOW_MIN,
                    criteria.missingDataPolicy(), failures);
        }
        return needsReview;
    }

    /** @return true when the caller should mark the outcome as needing human review. */
    private static boolean checkMin(BigDecimal actual, BigDecimal min, String field,
                                    String failureCode, MissingDataPolicy policy,
                                    List<String> failures) {
        if (min == null) {
            // Enabled with no threshold. The service rejects this at write time;
            // treating it as "no constraint" here is the safe reading, and it is
            // never silently a pass for a gate that does have a value.
            return false;
        }
        if (actual == null) {
            return applyMissingPolicy(policy, field, failures);
        }
        if (actual.compareTo(min) < 0) {
            failures.add(failureCode);
        }
        return false;
    }

    private static boolean checkMax(Integer actual, Integer max, String field,
                                    String failureCode, MissingDataPolicy policy,
                                    List<String> failures) {
        if (max == null) {
            return false;
        }
        if (actual == null) {
            return applyMissingPolicy(policy, field, failures);
        }
        if (actual > max) {
            failures.add(failureCode);
        }
        return false;
    }

    private static boolean applyMissingPolicy(MissingDataPolicy policy, String field, List<String> failures) {
        switch (policy) {
            case INELIGIBLE -> failures.add(EligibilityFailureCode.missingData(field));
            case NEEDS_REVIEW -> {
                return true;
            }
            case ELIGIBLE -> { /* explicitly waved through */ }
        }
        return false;
    }

    private static boolean meetsMin(BigDecimal actual, BigDecimal min) {
        return actual != null && (min == null || actual.compareTo(min) >= 0);
    }
}
