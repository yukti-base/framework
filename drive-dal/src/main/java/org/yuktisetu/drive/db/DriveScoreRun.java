package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.yuktisetu.drive.model.ScoreRunStatus;

import java.time.Instant;

/**
 * One drive's composite-scoring run: what it was given and how it ended.
 *
 * One row per drive. Unique on the posting AND on the DriveReadyForScoring event
 * that started it, so a redelivered event, or the reconciler racing the
 * consumer, can never produce a second run. A run held for TnP review is redone
 * in place when the TnP approves; it does not gain a sibling.
 *
 * config is a snapshot of the weights, threshold and floors the run used.
 * Students are shown the weights of THE RUN, never the drive's current config,
 * so a later edit can never change what someone was told produced their score.
 */
@Entity
@Table(
        name = "drive_score_runs",
        // One ACTIVE run per drive is a partial unique index (superseded_at IS
        // NULL) in .schema/2026-10-27-drive-reopen.sql; JPA cannot express it.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_score_run_event", columnNames = "ready_event_id")
        }
)
@Getter
@Setter
public class DriveScoreRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Column(name = "ready_event_id", length = 36)
    private String readyEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScoreRunStatus status;

    /** {"weights": {...}, "threshold": .., "floors": {...}} -- see ScoringRunService. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private String config;

    @Column(name = "pool_size", nullable = false)
    private int poolSize;

    @Column(name = "scored_count", nullable = false)
    private int scoredCount;

    @Column(name = "held_count", nullable = false)
    private int heldCount;

    @Column(name = "declined_reason", columnDefinition = "text")
    private String declinedReason;

    /** Factors excluded for the whole pool, and why. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pool_exclusions", columnDefinition = "jsonb")
    private String poolExclusions;

    /** studentUserId -> factor -> reason, when the run is HELD_FOR_REVIEW. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "held_students", columnDefinition = "jsonb")
    private String heldStudents;

    /** Coding platforms dropped from the comparison, and why. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coding_dropped", columnDefinition = "jsonb")
    private String codingDropped;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    /**
     * Set when the drive is reopened. A superseded run is kept, never edited --
     * it is the record of what students were told -- and no screen reads it as
     * the drive's current ranking. Every lookup asks for the run with this null.
     */
    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "superseded_by")
    private Long supersededBy;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
