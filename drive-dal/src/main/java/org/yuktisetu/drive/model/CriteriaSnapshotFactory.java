package org.yuktisetu.drive.model;

import org.yuktisetu.drive.db.PostingAptitudeCriterion;
import org.yuktisetu.drive.db.PostingCustomCriterion;
import org.yuktisetu.drive.db.PostingEligibilityCriteria;
import org.yuktisetu.drive.db.PostingSemesterCgpaCriterion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a CriteriaSnapshot from the criteria entities.
 *
 * Static and repository-free on purpose. Both admin-service (which resolves a
 * whole cohort at publish) and drive-service (which re-checks one student at apply
 * time) need identical criteria semantics, and the only way to guarantee that is
 * for both to call the same code. A Spring bean would have to live in one service
 * and be duplicated into the other, which is exactly how the preview count and the
 * enforced result drift apart -- the TnP publishes expecting 300 applicants and
 * gets 40, with no way to explain the gap.
 *
 * The caller loads the child rows however suits it (batched, cached, or in one
 * join) and hands them in; this class only assembles.
 */
public final class CriteriaSnapshotFactory {

    private CriteriaSnapshotFactory() {}

    public static CriteriaSnapshot of(PostingEligibilityCriteria c,
                                      List<PostingSemesterCgpaCriterion> semesterRows,
                                      List<PostingAptitudeCriterion> aptitudeRows,
                                      List<PostingCustomCriterion> customRows) {

        Map<Integer, BigDecimal> semesterGates = new LinkedHashMap<>();
        if (c.isSemesterCgpaEnabled() && semesterRows != null) {
            for (PostingSemesterCgpaCriterion s : semesterRows) {
                if (s.isEnabled() && !s.isDeleted()) {
                    semesterGates.put(s.getSemester().intValue(), s.getMinGpa());
                }
            }
        }

        // CoCubes is the only aptitude exam with a column on StudentProfile.
        // Every other one is reported as unevaluated rather than passed: a gate
        // that silently waves everyone through is worse than no gate, because the
        // TnP believes it is filtering.
        List<AptitudeExam> unevaluable = new ArrayList<>();
        if (aptitudeRows != null) {
            for (PostingAptitudeCriterion a : aptitudeRows) {
                if (a.isEnabled() && !a.isDeleted() && a.getExam() != AptitudeExam.COCUBES) {
                    unevaluable.add(a.getExam());
                }
            }
        }

        List<CriteriaSnapshot.CustomCriterion> customs = new ArrayList<>();
        if (customRows != null) {
            for (PostingCustomCriterion x : customRows) {
                if (!x.isEnabled() || x.isDeleted()) {
                    continue;
                }
                // Only DYNAMIC_FIELD criteria have a resolvable student-side value
                // today -- that is the Advanced Fields mechanism. STUDENT_PROFILE_FIELD
                // and CODING_PROFILE need a field-path resolver that does not exist
                // yet, and MANUAL is by definition a human check.
                boolean evaluable = x.getSource() == CriterionSource.DYNAMIC_FIELD;
                customs.add(new CriteriaSnapshot.CustomCriterion(
                        x.getCriterionKey(), x.getLabel(), x.getSource(), x.getOperator(),
                        x.getValueType(), x.getValueJson(), x.isMandatory(),
                        x.getMissingDataPolicy(), evaluable));
            }
        }

        return new CriteriaSnapshot(
                c.isAggregateCgpaEnabled(), c.getMinAggregateCgpa(),
                c.isSemesterCgpaEnabled(), Collections.unmodifiableMap(semesterGates),
                c.isTenthEnabled(), c.getMinTenthPercentage(),
                c.isTwelfthEnabled(), c.getMinTwelfthPercentage(),
                c.isDiplomaEnabled(), c.getMinDiplomaPercentage(),
                c.getTwelfthOrDiplomaMode(),
                c.isActiveBacklogsEnabled(), c.getMaxActiveBacklogs(),
                c.isTotalBacklogsEnabled(), c.getMaxTotalBacklogs(),
                c.isGapYearsEnabled(), c.getMaxGapYears(),
                c.isCocubesEnabled(), c.getMinCocubesScore(),
                List.copyOf(unevaluable), List.copyOf(customs),
                c.getMissingDataPolicy(),
                // SSOT 5.9's all-drive gate is not enforced yet: resume and
                // category/consent documents have no columns on StudentProfile, so
                // "the TnP's mandatory fields are complete" is not a question this
                // schema can answer. Turning it on would gate every drive on a
                // mostly vacuous check. Revisit when those fields land.
                false);
    }

    /** Which dynamic-field keys a run must resolve. Empty means skip that query entirely. */
    public static List<String> dynamicFieldKeys(CriteriaSnapshot snapshot) {
        return snapshot.customCriteria().stream()
                .filter(c -> c.source() == CriterionSource.DYNAMIC_FIELD)
                .map(CriteriaSnapshot.CustomCriterion::criterionKey)
                .distinct()
                .toList();
    }
}
