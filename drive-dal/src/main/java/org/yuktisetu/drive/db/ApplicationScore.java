package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One application's composite score for its drive.
 *
 * Mapped to the APPLICATION, and through it to exactly one drive, because a score
 * only means something against the pool it was computed over. The same student
 * has a different score on every drive, by design.
 *
 * breakdown is the scorer's own ScoringTrace, serialised. It is the same
 * structure that was logged, so the stored explanation and the log can never
 * disagree. rank is for the TnP's screen only and is never sent to a student.
 */
@Entity
@Table(
        name = "application_scores",
        // Per run, not per application: a reopened drive is ranked again, and the
        // superseded run keeps its own rows.
        uniqueConstraints = @UniqueConstraint(name = "uk_score_run_application", columnNames = {"run_id", "application_id"}),
        indexes = {
                @Index(name = "idx_score_posting_rank", columnList = "posting_id, rank"),
                @Index(name = "idx_score_student", columnList = "student_user_id")
        }
)
@Getter
@Setter
public class ApplicationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "composite", nullable = false, precision = 7, scale = 2)
    private BigDecimal composite;

    @Column(name = "meets_threshold", nullable = false)
    private boolean meetsThreshold;

    /** Meets the threshold and failed no floor. */
    @Column(name = "shortlist_eligible", nullable = false)
    private boolean shortlistEligible;

    /** Comma-separated ScoringFactor names; null when none failed. */
    @Column(name = "floors_failed", length = 255)
    private String floorsFailed;

    @Column(name = "weights_renormalised", nullable = false)
    private boolean weightsRenormalised;

    @Column(name = "rank", nullable = false)
    private int rank;

    /** Shares its rank with at least one other applicant. */
    @Column(name = "tied", nullable = false)
    private boolean tied;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "breakdown", nullable = false, columnDefinition = "jsonb")
    private String breakdown;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
