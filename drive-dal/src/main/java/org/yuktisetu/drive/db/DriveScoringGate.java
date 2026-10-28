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
import org.hibernate.annotations.UpdateTimestamp;
import org.yuktisetu.drive.model.ScoringGateState;

import java.time.Instant;

/**
 * May this drive be scored yet? One row per sealed COMPOSITE_SCORE drive.
 *
 * The row is also the LOCK that makes the READY transition happen exactly once.
 * Every action that can settle a condition -- sealing, a scrape finishing, the
 * reconciler, a TnP review decision -- takes this row FOR UPDATE before it looks
 * at anything else, then recounts and moves the state. Whichever of them settles
 * the last condition writes DriveReadyForScoring, in the same transaction.
 *
 * LOCK ORDER, which every caller must follow to stay deadlock-free: the posting
 * row (when the caller can race the seal), then the gate row, then capture rows.
 * Never the other way round.
 */
@Entity
@Table(
        name = "drive_scoring_gates",
        uniqueConstraints = @UniqueConstraint(name = "uk_scoring_gate_posting", columnNames = "posting_id")
)
@Getter
@Setter
public class DriveScoringGate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 24)
    private ScoringGateState state = ScoringGateState.COLLECTING;

    /** When the pool sealed. */
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    /** When the last capture in the pool settled. */
    @Column(name = "captures_settled_at")
    private Instant capturesSettledAt;

    /** When the TnP was asked to review. Set once, so the request is sent once. */
    @Column(name = "review_requested_at")
    private Instant reviewRequestedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    /** The DriveReadyForScoring event's id: the proof it was written, and only once. */
    @Column(name = "ready_event_id", length = 36)
    private String readyEventId;

    @Column(name = "pool_size", nullable = false)
    private int poolSize;

    @Column(name = "pending_count", nullable = false)
    private int pendingCount;

    @Column(name = "captured_count", nullable = false)
    private int capturedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
