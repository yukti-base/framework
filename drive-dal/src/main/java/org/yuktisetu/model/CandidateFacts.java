package org.yuktisetu.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Everything the academic half of the eligibility gate needs about one student,
 * flattened out of StudentProfile / User / UserRoleAssignment.
 *
 * Deliberately a plain value object with no JPA on it, so EligibilityEvaluator
 * stays a pure function that can be unit-tested without a database. Any field
 * may be null -- that is the whole reason MissingDataPolicy exists.
 */
public record CandidateFacts(
        Long studentUserId,
        Long matchedTargetId,
        Long collegeId,
        Long departmentId,
        Integer batchYear,

        BigDecimal aggregateCgpa,
        /** semester number (1-8) -> GPA. Absent keys mean no result recorded. */
        Map<Integer, BigDecimal> semesterGpa,
        BigDecimal tenthPercentage,
        BigDecimal twelfthPercentage,
        BigDecimal diplomaPercentage,
        Integer activeBacklogs,
        Integer totalBacklogs,
        Integer gapYears,
        BigDecimal cocubesScore,

        /** SSOT 5.9's all-drive gate: the TnP-mandated fields are filled in. */
        boolean profileComplete,

        /**
         * Resolved custom-criterion values, keyed by criterionKey. For
         * DYNAMIC_FIELD criteria these come from StudentDynamicFieldValue; a key
         * absent from this map means "no value on record", which
         * MissingDataPolicy then decides the meaning of.
         */
        Map<String, String> customValues
) {}
