package org.yuktisetu.drive.model;

import org.yuktisetu.profile.db.StudentProfile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Flattens a StudentProfile into the CandidateFacts the evaluator consumes.
 *
 * Static for the same reason as CriteriaSnapshotFactory: the bulk path and the
 * single-student apply path must read a profile identically, or a student can be
 * eligible in the list and rejected on apply for no visible reason.
 */
public final class CandidateFactsFactory {

    private CandidateFactsFactory() {}

    public static CandidateFacts of(StudentProfile profile,
                                    Long matchedTargetId,
                                    Long collegeId,
                                    Long departmentId,
                                    Map<String, String> customValues) {

        // StudentProfile denormalises semesters into eight columns, so the mapping
        // happens here in Java rather than as an unindexable SQL CASE. Sized to 8
        // up front so the map never rehashes.
        Map<Integer, BigDecimal> semesters = new HashMap<>(16);
        putSemester(semesters, 1, profile.getSem1Gpa());
        putSemester(semesters, 2, profile.getSem2Gpa());
        putSemester(semesters, 3, profile.getSem3Gpa());
        putSemester(semesters, 4, profile.getSem4Gpa());
        putSemester(semesters, 5, profile.getSem5Gpa());
        putSemester(semesters, 6, profile.getSem6Gpa());
        putSemester(semesters, 7, profile.getSem7Gpa());
        putSemester(semesters, 8, profile.getSem8Gpa());

        return new CandidateFacts(
                profile.getUser().getId(),
                matchedTargetId,
                collegeId,
                departmentId,
                profile.getGraduationYear(),
                dec(profile.getCgpa()),
                semesters,
                dec(profile.getTenthPercentage()),
                dec(profile.getTwelfthPercentage()),
                dec(profile.getDiplomaPercentage()),
                profile.getActiveBacklogs(),
                profile.getTotalBacklogs(),
                profile.getGapYears(),
                dec(profile.getCoCubesScore()),
                profile.getUser().getPhone() != null && !profile.getUser().getPhone().isBlank(),
                customValues == null ? Map.of() : customValues);
    }

    private static void putSemester(Map<Integer, BigDecimal> target, int semester, Double gpa) {
        if (gpa != null) {
            target.put(semester, BigDecimal.valueOf(gpa));
        }
    }

    private static BigDecimal dec(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
