package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.drive.model.WithdrawalRequestStatus;

import java.time.Instant;

/**
 * A student asking to give up an offer they accepted (decided 2026-11-01). The
 * offer stays ACCEPTED until the TnP approves; approval moves it to WITHDRAWN.
 *
 * At most one PENDING request per offer, enforced by the database:
 * pending_lock holds the offer id while the request is pending and is nulled when
 * it is decided -- the same unique-nullable trick as the internship lock, so a
 * double-click cannot file two requests.
 */
@Entity
@Table(
        name = "offer_withdrawal_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_withdrawal_pending_per_offer", columnNames = "pending_lock"),
        indexes = {
                @Index(name = "idx_withdrawal_offer", columnList = "offer_id"),
                @Index(name = "idx_withdrawal_posting_status", columnList = "posting_id, status")
        }
)
@Getter
@Setter
public class OfferWithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private WithdrawalRequestStatus status = WithdrawalRequestStatus.PENDING;

    /** The offer id while PENDING, null once decided. */
    @Column(name = "pending_lock")
    private Long pendingLock;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", length = 512)
    private String decisionNote;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
