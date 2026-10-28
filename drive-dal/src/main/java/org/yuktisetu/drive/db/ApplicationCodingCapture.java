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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.yuktisetu.drive.model.CaptureFlagReason;
import org.yuktisetu.drive.model.CaptureReviewState;
import org.yuktisetu.drive.model.CaptureState;
import org.yuktisetu.profile.model.CodingPlatform;

import java.time.Instant;

/**
 * What one application was scored on, for one coding platform.
 *
 * Created inside the apply transaction, so an application and its capture are
 * the same fact: there is no window in which a student has applied but nothing
 * has been asked to read their profile.
 *
 * Frozen at apply by decision. The snapshot this row points to is the one taken
 * when the student applied; practising afterwards does not change it. The one
 * exception is the student's own re-check of a handle that could not be read,
 * once per platform, before the deadline. A withdraw and re-apply SUPERSEDES the
 * old row and creates a new one, because that is a new application moment.
 *
 * Ids rather than associations on purpose: the event consumer and the gate
 * update these rows in bulk under a lock, and must never trigger a lazy load of
 * an application or a posting while holding it.
 */
@Entity
@Table(
        name = "application_coding_captures",
        indexes = {
                @Index(name = "idx_capture_application", columnList = "application_id"),
                @Index(name = "idx_capture_posting_state", columnList = "posting_id, state, review_state")
        }
)
@Getter
@Setter
public class ApplicationCodingCapture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private CodingPlatform platform;

    /** The handle as the student had entered it at the moment they applied. */
    @Column(name = "handle", length = 128)
    private String handle;

    /** The scrape job filling this capture. Shared when two applications land together. */
    @Column(name = "job_id")
    private Long jobId;

    /** Set on CAPTURED: the exact coding_profile_snapshots row this application is scored on. */
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private CaptureState state = CaptureState.PENDING;

    /** The scraper's verdict (OK, HANDLE_NOT_FOUND, PLATFORM_ERROR, ...), or NO_HANDLE. */
    @Column(name = "outcome", length = 24)
    private String outcome;

    @Column(name = "outcome_detail", columnDefinition = "text")
    private String outcomeDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_state", nullable = false, length = 16)
    private CaptureReviewState reviewState = CaptureReviewState.NOT_REQUIRED;

    /** Why the TnP is asked to look at this capture; null when it is not flagged. */
    @Enumerated(EnumType.STRING)
    @Column(name = "flag_reason", length = 24)
    private CaptureFlagReason flagReason;

    /** Re-checks the student has made. One is allowed per platform. */
    @Column(name = "recheck_count", nullable = false)
    private int recheckCount;

    /** The handle before the student's re-check, kept as the record of what they changed. */
    @Column(name = "previous_handle", length = 128)
    private String previousHandle;

    @Column(name = "rechecked_at")
    private Instant recheckedAt;

    /** When the student applied -- the moment this capture stands for. */
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Settle as captured. */
    public void markCaptured(Long snapshotId, Instant at) {
        this.state = CaptureState.CAPTURED;
        this.snapshotId = snapshotId;
        this.outcome = "OK";
        this.outcomeDetail = null;
        this.reviewState = CaptureReviewState.NOT_REQUIRED;
        this.flagReason = null;
        this.settledAt = at;
    }

    /** Failed, and the TnP must decide. */
    public void markFlagged(String outcome, String detail, CaptureFlagReason reason, Instant at) {
        this.state = CaptureState.FAILED;
        this.outcome = outcome;
        this.outcomeDetail = detail;
        this.reviewState = CaptureReviewState.NEEDS_REVIEW;
        this.flagReason = reason;
        this.settledAt = at;
    }

    /** The handle could not be read; the student is asked to re-check it. */
    public void markAwaitingStudent(String outcome, String detail, Instant at) {
        this.state = CaptureState.AWAITING_STUDENT;
        this.outcome = outcome;
        this.outcomeDetail = detail;
        this.reviewState = CaptureReviewState.NOT_REQUIRED;
        this.flagReason = null;
        this.settledAt = at;
    }

    /**
     * The re-check failed on the handle too. Not flagged: the student is scored
     * on their other platforms. If they turn out to have none, the gate flags
     * this row NOTHING_LEFT when the pool settles.
     */
    public void markFailedScoredOnOthers(String outcome, String detail, Instant at) {
        this.state = CaptureState.FAILED;
        this.outcome = outcome;
        this.outcomeDetail = detail;
        this.reviewState = CaptureReviewState.NOT_REQUIRED;
        this.flagReason = null;
        this.settledAt = at;
    }

    /** The student re-checked: read again, possibly under a corrected handle. */
    public void startRecheck(String newHandle, Long newJobId, Instant at) {
        this.previousHandle = this.handle;
        this.handle = newHandle;
        this.jobId = newJobId;
        this.recheckCount++;
        this.recheckedAt = at;
        this.state = CaptureState.PENDING;
        this.outcome = null;
        this.outcomeDetail = null;
        this.settledAt = null;
    }

    /** Moved to the TnP after the fact: no re-check by the deadline, or nothing left to score on. */
    public void escalate(CaptureFlagReason reason, Instant at) {
        this.state = CaptureState.FAILED;
        this.reviewState = CaptureReviewState.NEEDS_REVIEW;
        this.flagReason = reason;
        if (this.settledAt == null) {
            this.settledAt = at;
        }
    }
}
