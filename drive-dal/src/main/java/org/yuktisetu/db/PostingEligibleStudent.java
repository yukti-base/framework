package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * The materialised answer to "who may see and apply to this posting". One row
 * per ELIGIBLE student only -- ineligible students are absent, not flagged.
 *
 * This is what makes the two hard requirements the same set by construction:
 * the students who get notified and the students whose drive list includes the
 * posting are both exactly the rows in this table. The student list endpoint
 * becomes an indexed join instead of evaluating criteria for every posting on
 * every page load.
 *
 * It is an INDEX, not the authority. The apply endpoint re-evaluates eligibility
 * live against the student's current profile, because this row could have been
 * written days earlier.
 */
@Entity
@Table(
        name = "posting_eligible_students",
        uniqueConstraints = @UniqueConstraint(name = "uk_eligible_posting_student", columnNames = {"posting_id", "student_user_id"}),
        indexes = {
                // "which drives can I see" -- the student's whole listing query.
                @Index(name = "idx_eligible_student_user_id", columnList = "student_user_id"),
                @Index(name = "idx_eligible_posting_id", columnList = "posting_id")
        }
)
@Getter
@Setter
public class PostingEligibleStudent extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eligible_student_posting"))
    private JobPosting posting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eligible_student_user"))
    private User student;

    /** Which PostingTarget row admitted them -- useful when a drive spans colleges. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_target_id",
            foreignKey = @ForeignKey(name = "fk_eligible_student_target"))
    private PostingTarget matchedTarget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_run_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eligible_student_run"))
    private EligibilityRun evaluationRun;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "notified", nullable = false)
    private boolean notified = false;

    public Long getStudentUserId() { return student != null ? student.getId() : null; }

    public Long getMatchedTargetId() { return matchedTarget != null ? matchedTarget.getId() : null; }

    public Long getEvaluationRunId() { return evaluationRun != null ? evaluationRun.getId() : null; }
}
