package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yuktisetu.identity.db.User;
import org.yuktisetu.profile.db.StudentProfile;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The equivalence contract for Phase 4 of the migration.
 *
 * Today `CandidateFactsFactory.of(...)` takes a `StudentProfile` JPA entity and
 * reaches through it to `User`. Phase 4 replaces that parameter with a
 * `StudentFacts` value object fed from the `student_facts` replica, which is the
 * single change that detaches the eligibility engine from profile-dal.
 *
 * This class pins the EXACT `CandidateFacts` the current implementation
 * produces. When the signature changes, these assertions must still hold from
 * the new input — that is the proof the replica carries everything the rules
 * need. Assertions are written field-by-field against literal expected values
 * precisely so they can be reused verbatim against the new input type.
 */
class CandidateFactsFactoryTest {

    private static User user(Long id, String phone) {
        return User.builder().id(id).phone(phone).build();
    }

    /** A profile with every academic field populated. */
    private static StudentProfile fullProfile(User u) {
        return StudentProfile.builder()
                .user(u)
                .graduationYear(2027)
                .cgpa(7.85)
                .sem1Gpa(7.1).sem2Gpa(7.2).sem3Gpa(7.3).sem4Gpa(7.4)
                .sem5Gpa(7.5).sem6Gpa(7.6).sem7Gpa(7.7).sem8Gpa(7.8)
                .tenthPercentage(88.5)
                .twelfthPercentage(76.25)
                .diplomaPercentage(71.0)
                .activeBacklogs(0)
                .totalBacklogs(2)
                .gapYears(1)
                .coCubesScore(620.0)
                .build();
    }

    @Test
    @DisplayName("every academic field maps across unchanged")
    void allFieldsMap() {
        CandidateFacts f = CandidateFactsFactory.of(
                fullProfile(user(42L, "9876543210")), 99L, 5L, 6L, Map.of("k", "v"));

        assertEquals(42L, f.studentUserId());
        assertEquals(99L, f.matchedTargetId());
        assertEquals(5L, f.collegeId());
        assertEquals(6L, f.departmentId());
        assertEquals(2027, f.batchYear());

        assertEquals(0, new BigDecimal("7.85").compareTo(f.aggregateCgpa()));
        assertEquals(0, new BigDecimal("88.5").compareTo(f.tenthPercentage()));
        assertEquals(0, new BigDecimal("76.25").compareTo(f.twelfthPercentage()));
        assertEquals(0, new BigDecimal("71.0").compareTo(f.diplomaPercentage()));
        assertEquals(0, new BigDecimal("620.0").compareTo(f.cocubesScore()));

        assertEquals(0, f.activeBacklogs());
        assertEquals(2, f.totalBacklogs());
        assertEquals(1, f.gapYears());
        assertEquals(Map.of("k", "v"), f.customValues());
    }

    @Test
    @DisplayName("all eight semesters land under keys 1..8")
    void semestersAreKeyedByNumber() {
        CandidateFacts f = CandidateFactsFactory.of(
                fullProfile(user(1L, "9")), null, null, null, null);

        assertEquals(8, f.semesterGpa().size());
        assertEquals(0, new BigDecimal("7.1").compareTo(f.semesterGpa().get(1)));
        assertEquals(0, new BigDecimal("7.8").compareTo(f.semesterGpa().get(8)));
    }

    /**
     * The distinction the eligibility engine depends on: an ABSENT key means "no
     * result recorded" and routes through the missing-data policy, whereas a
     * present 0.0 would be read as a real GPA of zero and fail every gate. A
     * replica that defaults unset semesters to zero would silently make
     * first-year students ineligible everywhere.
     */
    @Test
    @DisplayName("an unrecorded semester is ABSENT from the map, never zero")
    void unrecordedSemesterIsAbsentNotZero() {
        StudentProfile partial = StudentProfile.builder()
                .user(user(7L, "9"))
                .sem1Gpa(8.0)
                .sem2Gpa(null)
                .build();

        CandidateFacts f = CandidateFactsFactory.of(partial, null, null, null, null);

        assertEquals(1, f.semesterGpa().size(), "only the recorded semester is present");
        assertTrue(f.semesterGpa().containsKey(1));
        assertFalse(f.semesterGpa().containsKey(2),
                "an unrecorded semester must be absent, not stored as 0.0");
        assertNull(f.semesterGpa().get(2));
    }

    @Test
    @DisplayName("null academic values stay null rather than becoming zero")
    void nullsSurviveAsNulls() {
        StudentProfile empty = StudentProfile.builder().user(user(8L, "9")).build();

        CandidateFacts f = CandidateFactsFactory.of(empty, null, null, null, null);

        assertNull(f.aggregateCgpa(), "a missing CGPA must not become 0.0 and fail every gate");
        assertNull(f.tenthPercentage());
        assertNull(f.twelfthPercentage());
        assertNull(f.diplomaPercentage());
        assertNull(f.cocubesScore());
        assertNull(f.activeBacklogs());
        assertNull(f.totalBacklogs());
        assertNull(f.gapYears());
        assertNull(f.batchYear());
        assertTrue(f.semesterGpa().isEmpty());
    }

    // ------------------------------------------------ the identity-derived gate

    /**
     * `profileComplete` is the SSOT 5.9 all-drive gate, and it is computed from
     * `User.phone` — an IDENTITY field, reached off a PROFILE entity, inside a
     * DRIVE module. It is the single most fragile value in the migration: the
     * replica must carry a phone-PRESENT boolean, not the phone itself, and not
     * omit it.
     */
    @Test
    @DisplayName("profileComplete is true when the user has a phone number")
    void phonePresentMeansComplete() {
        CandidateFacts f = CandidateFactsFactory.of(
                fullProfile(user(1L, "9876543210")), null, null, null, null);
        assertTrue(f.profileComplete());
    }

    @Test
    @DisplayName("profileComplete is false when the phone is null")
    void nullPhoneMeansIncomplete() {
        CandidateFacts f = CandidateFactsFactory.of(
                fullProfile(user(1L, null)), null, null, null, null);
        assertFalse(f.profileComplete());
    }

    @Test
    @DisplayName("profileComplete is false when the phone is blank or whitespace")
    void blankPhoneMeansIncomplete() {
        assertFalse(CandidateFactsFactory.of(
                fullProfile(user(1L, "")), null, null, null, null).profileComplete());
        assertFalse(CandidateFactsFactory.of(
                fullProfile(user(1L, "   ")), null, null, null, null).profileComplete(),
                "whitespace is not a phone number");
    }

    @Test
    @DisplayName("null customValues becomes an empty map, never null")
    void nullCustomValuesBecomesEmptyMap() {
        CandidateFacts f = CandidateFactsFactory.of(
                fullProfile(user(1L, "9")), null, null, null, null);

        assertEquals(Map.of(), f.customValues(),
                "the evaluator iterates this map; null would throw at drive time");
    }

    // ------------------------------------------------- end-to-end equivalence

    /**
     * The assertion that matters most: facts built by the factory, fed straight
     * into the evaluator, produce the eligibility decision the TnP expects. If
     * Phase 4 changes the factory's input and this still passes, the replica is
     * carrying enough.
     */
    @Test
    @DisplayName("factory output drives the evaluator to the same verdict end to end")
    void factoryFeedsEvaluatorConsistently() {
        CandidateFacts strong = CandidateFactsFactory.of(
                fullProfile(user(1L, "9876543210")), null, null, null, null);
        CandidateFacts noPhone = CandidateFactsFactory.of(
                fullProfile(user(2L, null)), null, null, null, null);

        CriteriaSnapshot requiresComplete = new CriteriaSnapshot(
                true, new BigDecimal("7.0"),
                false, Map.of(),
                false, null, false, null, false, null,
                AlternativeGateMode.EITHER_SUFFICIENT,
                true, 0,
                false, null, false, null, false, null,
                java.util.List.of(), java.util.List.of(),
                MissingDataPolicy.INELIGIBLE,
                true);

        assertTrue(EligibilityEvaluator.evaluate(requiresComplete, strong).eligible(),
                "7.85 CGPA, 0 active backlogs, phone present -> eligible");

        var out = EligibilityEvaluator.evaluate(requiresComplete, noPhone);
        assertFalse(out.eligible(), "identical academics but no phone -> SSOT 5.9 rejects");
        assertTrue(out.failureCodes().contains(EligibilityFailureCode.PROFILE_INCOMPLETE));
    }
}
