package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.model.EligibilityRunReason;
import org.yuktisetu.model.EligibilityRunStatus;

import java.time.Instant;

/**
 * One evaluation of a posting's criteria against its targeted cohort.
 *
 * Kept as a first-class row so "why did only 40 of 300 students qualify" is
 * answerable from data rather than by re-deriving it months later against a
 * profile set that has since changed. failureSummary holds counts per failure
 * code, which gives the TnP that answer without storing a row per rejected
 * student.
 */
@Entity
@Table(
        name = "eligibility_runs",
        indexes = @Index(name = "idx_eligibility_run_posting_id", columnList = "posting_id")
)
@Getter
@Setter
public class EligibilityRun extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eligibility_run_posting"))
    private JobPosting posting;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_reason", nullable = false, length = 24)
    private EligibilityRunReason runReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private EligibilityRunStatus status = EligibilityRunStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "candidates_evaluated")
    private Integer candidatesEvaluated;

    @Column(name = "eligible_count")
    private Integer eligibleCount;

    @Column(name = "ineligible_count")
    private Integer ineligibleCount;

    /** {"CGPA_BELOW_MIN": 112, "ACTIVE_BACKLOGS_EXCEEDED": 47, ...} */
    // @JdbcTypeCode(SqlTypes.JSON) is load-bearing, not decoration: Postgres will
    // not implicitly cast a bound varchar into a jsonb column, so without it every
    // insert fails with "column is of type jsonb but expression is of type
    // character varying". The column stays a String in Java -- the value is
    // already serialized JSON -- and Hibernate binds it as jsonb.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failure_summary", columnDefinition = "jsonb")
    private String failureSummary;

    @Column(name = "error", columnDefinition = "text")
    private String error;
}
