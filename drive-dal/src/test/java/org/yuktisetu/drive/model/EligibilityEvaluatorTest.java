package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The regression net for the one thing the microservices migration must not
 * change: who is eligible for a drive.
 *
 * EligibilityEvaluator is a pure function over CriteriaSnapshot + CandidateFacts,
 * which is exactly why it can be pinned cheaply and completely. Every refactor in
 * the migration -- severing foreign keys, moving fact assembly behind a replica,
 * re-typing CandidateFactsFactory -- changes how these two records are BUILT. If
 * the behaviour encoded here still holds afterwards, the rules survived.
 *
 * A failure in this class means a student's eligibility changed. That is the
 * "silent failure" the whole exercise is meant to prevent.
 */
class EligibilityEvaluatorTest {

    // ---------------------------------------------------------------- helpers

    /** Every gate off, nothing required. The base for turning on one gate at a time. */
    private static CriteriaSnapshot.CustomCriterion[] noCustom() {
        return new CriteriaSnapshot.CustomCriterion[0];
    }

    private static CriteriaSnapshot criteria(MissingDataPolicy policy, boolean requireCompleteProfile) {
        return new CriteriaSnapshot(
                false, null,
                false, Map.of(),
                false, null,
                false, null,
                false, null,
                AlternativeGateMode.EITHER_SUFFICIENT,
                false, null,
                false, null,
                false, null,
                false, null,
                List.of(),
                List.of(noCustom()),
                policy,
                requireCompleteProfile);
    }

    private static CriteriaSnapshot allGatesOff() {
        return criteria(MissingDataPolicy.INELIGIBLE, false);
    }

    private static CandidateFacts facts(BigDecimal cgpa, Integer activeBacklogs, boolean profileComplete) {
        return new CandidateFacts(
                1L, 10L, 100L, 200L, 2027,
                cgpa,
                Map.of(),
                null, null, null,
                activeBacklogs, null, null, null,
                profileComplete,
                Map.of());
    }

    /** A student with nothing recorded — the missing-data path. */
    private static CandidateFacts emptyFacts() {
        return facts(null, null, true);
    }

    // ---------------------------------------------------------------- baseline

    @Test
    @DisplayName("no gates enabled -> eligible, no failures, no review")
    void noGatesMeansEligible() {
        EligibilityOutcome out = EligibilityEvaluator.evaluate(allGatesOff(), facts(new BigDecimal("7.0"), 0, true));

        assertTrue(out.eligible());
        assertFalse(out.needsReview());
        assertTrue(out.failureCodes().isEmpty());
        assertFalse(out.hasFailures());
    }

    @Test
    @DisplayName("a disabled gate is ignored even when the student would fail it")
    void disabledGateIsNotEvaluated() {
        // cgpa 4.0 would fail any sane minimum, but the gate is off.
        EligibilityOutcome out = EligibilityEvaluator.evaluate(allGatesOff(), facts(new BigDecimal("4.0"), 99, true));

        assertTrue(out.eligible(), "a disabled criterion must never make a student ineligible");
    }

    // ------------------------------------------------- SSOT 5.9 profile gate

    @Nested
    @DisplayName("SSOT 5.9 profile-completeness gate")
    class ProfileCompleteness {

        /**
         * This is the single most fragile behaviour in the migration.
         * CandidateFacts.profileComplete is currently derived from User.phone --
         * an IDENTITY field read off a PROFILE entity inside a DRIVE module. When
         * fact assembly moves behind the student_facts replica, a replica that
         * forgets to carry "phone present" would silently flip this gate for
         * every student, on every drive.
         */
        @Test
        @DisplayName("incomplete profile fails EVERY drive, even with all other gates off")
        void incompleteProfileFailsEveryDrive() {
            CriteriaSnapshot c = criteria(MissingDataPolicy.INELIGIBLE, true);

            EligibilityOutcome out = EligibilityEvaluator.evaluate(c, facts(new BigDecimal("9.5"), 0, false));

            assertFalse(out.eligible(), "SSOT 5.9 gate must reject regardless of academic strength");
            assertTrue(out.failureCodes().contains(EligibilityFailureCode.PROFILE_INCOMPLETE));
        }

        @Test
        @DisplayName("complete profile clears the gate")
        void completeProfilePasses() {
            CriteriaSnapshot c = criteria(MissingDataPolicy.INELIGIBLE, true);

            assertTrue(EligibilityEvaluator.evaluate(c, facts(new BigDecimal("6.0"), 0, true)).eligible());
        }

        @Test
        @DisplayName("when the drive does not require it, an incomplete profile is irrelevant")
        void gateNotAppliedWhenNotRequired() {
            CriteriaSnapshot c = criteria(MissingDataPolicy.INELIGIBLE, false);

            assertTrue(EligibilityEvaluator.evaluate(c, facts(new BigDecimal("6.0"), 0, false)).eligible());
        }
    }

    // ------------------------------------------------------------- CGPA gate

    @Nested
    @DisplayName("aggregate CGPA")
    class AggregateCgpa {

        private CriteriaSnapshot withMinCgpa(BigDecimal min, MissingDataPolicy policy) {
            CriteriaSnapshot b = criteria(policy, false);
            return new CriteriaSnapshot(
                    true, min,
                    b.semesterCgpaEnabled(), b.semesterMinGpa(),
                    b.tenthEnabled(), b.minTenthPercentage(),
                    b.twelfthEnabled(), b.minTwelfthPercentage(),
                    b.diplomaEnabled(), b.minDiplomaPercentage(),
                    b.twelfthOrDiplomaMode(),
                    b.activeBacklogsEnabled(), b.maxActiveBacklogs(),
                    b.totalBacklogsEnabled(), b.maxTotalBacklogs(),
                    b.gapYearsEnabled(), b.maxGapYears(),
                    b.cocubesEnabled(), b.minCocubesScore(),
                    b.unevaluableAptitudeExams(), b.customCriteria(),
                    b.missingDataPolicy(), b.requireCompleteProfile());
        }

        @Test
        @DisplayName("above the minimum is eligible")
        void aboveMinimum() {
            var out = EligibilityEvaluator.evaluate(
                    withMinCgpa(new BigDecimal("6.5"), MissingDataPolicy.INELIGIBLE),
                    facts(new BigDecimal("7.2"), 0, true));
            assertTrue(out.eligible());
        }

        @Test
        @DisplayName("exactly at the minimum is eligible -- the boundary is inclusive")
        void exactlyAtMinimumIsInclusive() {
            var out = EligibilityEvaluator.evaluate(
                    withMinCgpa(new BigDecimal("6.5"), MissingDataPolicy.INELIGIBLE),
                    facts(new BigDecimal("6.5"), 0, true));
            assertTrue(out.eligible(), "a student exactly on the cut-off must not be excluded");
        }

        @Test
        @DisplayName("below the minimum fails with CGPA_BELOW_MIN")
        void belowMinimum() {
            var out = EligibilityEvaluator.evaluate(
                    withMinCgpa(new BigDecimal("6.5"), MissingDataPolicy.INELIGIBLE),
                    facts(new BigDecimal("6.49"), 0, true));

            assertFalse(out.eligible());
            assertTrue(out.failureCodes().contains(EligibilityFailureCode.CGPA_BELOW_MIN));
        }

        @Test
        @DisplayName("missing CGPA under INELIGIBLE policy -> ineligible, not a crash")
        void missingUnderIneligiblePolicy() {
            var out = EligibilityEvaluator.evaluate(
                    withMinCgpa(new BigDecimal("6.5"), MissingDataPolicy.INELIGIBLE),
                    emptyFacts());

            assertFalse(out.eligible());
            assertFalse(out.needsReview());
        }

        @Test
        @DisplayName("missing CGPA under NEEDS_REVIEW policy -> flagged for review, not failed")
        void missingUnderNeedsReviewPolicy() {
            var out = EligibilityEvaluator.evaluate(
                    withMinCgpa(new BigDecimal("6.5"), MissingDataPolicy.NEEDS_REVIEW),
                    emptyFacts());

            assertTrue(out.needsReview(), "NEEDS_REVIEW must surface a human decision, not a silent rejection");
            assertTrue(out.failureCodes().isEmpty());
        }

        @Test
        @DisplayName("missing CGPA under ELIGIBLE policy -> waved through")
        void missingUnderEligiblePolicy() {
            var out = EligibilityEvaluator.evaluate(
                    withMinCgpa(new BigDecimal("6.5"), MissingDataPolicy.ELIGIBLE),
                    emptyFacts());

            assertTrue(out.eligible());
            assertFalse(out.needsReview());
        }
    }

    // --------------------------------------------------------- backlogs gate

    @Nested
    @DisplayName("active backlogs (a maximum, not a minimum)")
    class ActiveBacklogs {

        private CriteriaSnapshot withMaxActiveBacklogs(Integer max) {
            CriteriaSnapshot b = allGatesOff();
            return new CriteriaSnapshot(
                    b.aggregateCgpaEnabled(), b.minAggregateCgpa(),
                    b.semesterCgpaEnabled(), b.semesterMinGpa(),
                    b.tenthEnabled(), b.minTenthPercentage(),
                    b.twelfthEnabled(), b.minTwelfthPercentage(),
                    b.diplomaEnabled(), b.minDiplomaPercentage(),
                    b.twelfthOrDiplomaMode(),
                    true, max,
                    b.totalBacklogsEnabled(), b.maxTotalBacklogs(),
                    b.gapYearsEnabled(), b.maxGapYears(),
                    b.cocubesEnabled(), b.minCocubesScore(),
                    b.unevaluableAptitudeExams(), b.customCriteria(),
                    b.missingDataPolicy(), b.requireCompleteProfile());
        }

        @Test
        @DisplayName("zero backlogs against a zero cap is eligible")
        void zeroAgainstZero() {
            assertTrue(EligibilityEvaluator.evaluate(
                    withMaxActiveBacklogs(0), facts(new BigDecimal("7.0"), 0, true)).eligible());
        }

        @Test
        @DisplayName("one backlog against a zero cap fails -- the TnP's most-used gate")
        void oneAgainstZero() {
            var out = EligibilityEvaluator.evaluate(
                    withMaxActiveBacklogs(0), facts(new BigDecimal("7.0"), 1, true));

            assertFalse(out.eligible());
            assertTrue(out.failureCodes().contains(EligibilityFailureCode.ACTIVE_BACKLOGS_EXCEEDED));
        }

        @Test
        @DisplayName("null backlogs is missing data, not zero -- bulk-imported students have no value")
        void nullIsMissingNotZero() {
            var out = EligibilityEvaluator.evaluate(
                    withMaxActiveBacklogs(0), facts(new BigDecimal("7.0"), null, true));

            assertFalse(out.eligible(),
                    "a null backlog count must not be silently treated as zero backlogs");
        }
    }

    // ------------------------------------------- 12th / diploma alternative

    @Nested
    @DisplayName("12th-or-diploma alternative gate (EITHER_SUFFICIENT)")
    class TwelfthOrDiploma {

        private CriteriaSnapshot bothRoutes(BigDecimal minTwelfth, BigDecimal minDiploma, MissingDataPolicy policy) {
            CriteriaSnapshot b = criteria(policy, false);
            return new CriteriaSnapshot(
                    b.aggregateCgpaEnabled(), b.minAggregateCgpa(),
                    b.semesterCgpaEnabled(), b.semesterMinGpa(),
                    b.tenthEnabled(), b.minTenthPercentage(),
                    true, minTwelfth,
                    true, minDiploma,
                    AlternativeGateMode.EITHER_SUFFICIENT,
                    b.activeBacklogsEnabled(), b.maxActiveBacklogs(),
                    b.totalBacklogsEnabled(), b.maxTotalBacklogs(),
                    b.gapYearsEnabled(), b.maxGapYears(),
                    b.cocubesEnabled(), b.minCocubesScore(),
                    b.unevaluableAptitudeExams(), b.customCriteria(),
                    b.missingDataPolicy(), b.requireCompleteProfile());
        }

        private CandidateFacts withSchooling(BigDecimal twelfth, BigDecimal diploma) {
            return new CandidateFacts(
                    1L, 10L, 100L, 200L, 2027,
                    new BigDecimal("7.0"), Map.of(),
                    null, twelfth, diploma,
                    0, 0, 0, null,
                    true, Map.of());
        }

        @Test
        @DisplayName("passing the 12th route alone is sufficient")
        void twelfthAloneSuffices() {
            assertTrue(EligibilityEvaluator.evaluate(
                    bothRoutes(new BigDecimal("60"), new BigDecimal("60"), MissingDataPolicy.INELIGIBLE),
                    withSchooling(new BigDecimal("75"), null)).eligible());
        }

        @Test
        @DisplayName("passing the diploma route alone is sufficient -- the diploma-entry student")
        void diplomaAloneSuffices() {
            assertTrue(EligibilityEvaluator.evaluate(
                    bothRoutes(new BigDecimal("60"), new BigDecimal("60"), MissingDataPolicy.INELIGIBLE),
                    withSchooling(null, new BigDecimal("72"))).eligible(),
                    "a diploma-entry student with no 12th record must not be rejected");
        }

        @Test
        @DisplayName("failing both routes fails the gate")
        void failingBothFails() {
            var out = EligibilityEvaluator.evaluate(
                    bothRoutes(new BigDecimal("60"), new BigDecimal("60"), MissingDataPolicy.INELIGIBLE),
                    withSchooling(new BigDecimal("55"), new BigDecimal("50")));

            assertFalse(out.eligible());
            assertTrue(out.failureCodes().contains(EligibilityFailureCode.TWELFTH_OR_DIPLOMA_BELOW_MIN));
        }

        @Test
        @DisplayName("neither value recorded is MISSING DATA, not a failed gate")
        void neitherRecordedIsMissingData() {
            var review = EligibilityEvaluator.evaluate(
                    bothRoutes(new BigDecimal("60"), new BigDecimal("60"), MissingDataPolicy.NEEDS_REVIEW),
                    withSchooling(null, null));

            assertTrue(review.needsReview(),
                    "no schooling record at all must route to review, not be reported as below the cut-off");
            assertFalse(review.failureCodes().contains(EligibilityFailureCode.TWELFTH_OR_DIPLOMA_BELOW_MIN));
        }
    }

    // -------------------------------------------------- accumulation of failures

    @Test
    @DisplayName("every failed gate is reported, not just the first -- students are told all reasons")
    void allFailuresAccumulate() {
        CriteriaSnapshot b = criteria(MissingDataPolicy.INELIGIBLE, true);
        CriteriaSnapshot strict = new CriteriaSnapshot(
                true, new BigDecimal("8.0"),
                b.semesterCgpaEnabled(), b.semesterMinGpa(),
                b.tenthEnabled(), b.minTenthPercentage(),
                b.twelfthEnabled(), b.minTwelfthPercentage(),
                b.diplomaEnabled(), b.minDiplomaPercentage(),
                b.twelfthOrDiplomaMode(),
                true, 0,
                b.totalBacklogsEnabled(), b.maxTotalBacklogs(),
                b.gapYearsEnabled(), b.maxGapYears(),
                b.cocubesEnabled(), b.minCocubesScore(),
                b.unevaluableAptitudeExams(), b.customCriteria(),
                b.missingDataPolicy(), b.requireCompleteProfile());

        var out = EligibilityEvaluator.evaluate(strict, facts(new BigDecimal("5.0"), 3, false));

        assertFalse(out.eligible());
        assertTrue(out.failureCodes().contains(EligibilityFailureCode.PROFILE_INCOMPLETE));
        assertTrue(out.failureCodes().contains(EligibilityFailureCode.CGPA_BELOW_MIN));
        assertTrue(out.failureCodes().contains(EligibilityFailureCode.ACTIVE_BACKLOGS_EXCEEDED));
        assertEquals(3, out.failureCodes().size(), "exactly the three failed gates, no more");
    }
}
