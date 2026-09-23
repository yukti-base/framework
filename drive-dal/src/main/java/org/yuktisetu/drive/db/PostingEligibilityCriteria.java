package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.drive.model.AlternativeGateMode;
import org.yuktisetu.drive.model.MissingDataPolicy;

import java.math.BigDecimal;
import org.yuktisetu.persistence.BaseAuditableEntity;

/**
 * The hard eligibility gate for one posting -- the typed half of the hybrid
 * criteria model. Company one-offs live in PostingCustomCriterion instead.
 *
 * Every gate is a nullable threshold PLUS its own boolean enable flag. That
 * pairing is the whole point: a disabled criterion is SKIPPED by the evaluator,
 * it is not "a threshold of zero". Without the flag there is no way to express
 * "this drive does not care about 12th marks" distinctly from "this drive
 * requires at least 0%", and every un-set gate would silently pass everyone.
 *
 * Its own table rather than columns on job_postings so the posting row stays
 * narrow and a criteria set can be cloned or versioned later.
 */
@Entity
@Table(
        name = "posting_eligibility_criteria",
        uniqueConstraints = @UniqueConstraint(name = "uk_criteria_posting", columnNames = "posting_id")
)
@Getter
@Setter
public class PostingEligibilityCriteria extends BaseAuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_criteria_posting"))
    private JobPosting posting;

    // --- Aggregate CGPA (StudentProfile.cgpa) ---
    @Column(name = "aggregate_cgpa_enabled", nullable = false)
    private boolean aggregateCgpaEnabled = false;

    @Column(name = "min_aggregate_cgpa", precision = 4, scale = 2)
    private BigDecimal minAggregateCgpa;

    // --- Semester-wise CGPA: thresholds live in PostingSemesterCgpaCriterion ---
    @Column(name = "semester_cgpa_enabled", nullable = false)
    private boolean semesterCgpaEnabled = false;

    // --- 10th (StudentProfile.tenthPercentage) ---
    @Column(name = "tenth_enabled", nullable = false)
    private boolean tenthEnabled = false;

    @Column(name = "min_tenth_percentage", precision = 5, scale = 2)
    private BigDecimal minTenthPercentage;

    // --- 12th (StudentProfile.twelfthPercentage) ---
    @Column(name = "twelfth_enabled", nullable = false)
    private boolean twelfthEnabled = false;

    @Column(name = "min_twelfth_percentage", precision = 5, scale = 2)
    private BigDecimal minTwelfthPercentage;

    // --- Diploma (StudentProfile.diplomaPercentage) ---
    @Column(name = "diploma_enabled", nullable = false)
    private boolean diplomaEnabled = false;

    @Column(name = "min_diploma_percentage", precision = 5, scale = 2)
    private BigDecimal minDiplomaPercentage;

    /**
     * How the 12th and diploma gates combine when both are enabled. Diploma-lateral
     * students have no 12th marks at all, so requiring both would silently exclude
     * that entire cohort -- which is why EITHER_SUFFICIENT is the only mode today.
     * Kept as a column so a stricter combination can be added later without
     * reshaping this table.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "twelfth_or_diploma_mode", nullable = false, length = 20)
    private AlternativeGateMode twelfthOrDiplomaMode = AlternativeGateMode.EITHER_SUFFICIENT;

    // --- Backlogs (StudentProfile.activeBacklogs / totalBacklogs) ---
    @Column(name = "active_backlogs_enabled", nullable = false)
    private boolean activeBacklogsEnabled = false;

    @Column(name = "max_active_backlogs")
    private Integer maxActiveBacklogs;

    @Column(name = "total_backlogs_enabled", nullable = false)
    private boolean totalBacklogsEnabled = false;

    @Column(name = "max_total_backlogs")
    private Integer maxTotalBacklogs;

    // --- Gap years (StudentProfile.gapYears) ---
    @Column(name = "gap_years_enabled", nullable = false)
    private boolean gapYearsEnabled = false;

    @Column(name = "max_gap_years")
    private Integer maxGapYears;

    // --- CoCubes (StudentProfile.coCubesScore). Other aptitude exams go in
    //     PostingAptitudeCriterion; CoCubes gets a column because it is the only
    //     one with a student-side field today. ---
    @Column(name = "cocubes_enabled", nullable = false)
    private boolean cocubesEnabled = false;

    @Column(name = "min_cocubes_score", precision = 6, scale = 2)
    private BigDecimal minCocubesScore;

    /**
     * What an ENABLED gate does when the student's value is null. Explicit per
     * drive because either default is a trap: pass-on-null lets an incomplete
     * profile clear every gate, fail-on-null lets one un-synced ERP column
     * quietly remove a student from every drive in the season.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "missing_data_policy", nullable = false, length = 16)
    private MissingDataPolicy missingDataPolicy = MissingDataPolicy.INELIGIBLE;

    @Column(name = "criteria_note", columnDefinition = "text")
    private String criteriaNote;
}
