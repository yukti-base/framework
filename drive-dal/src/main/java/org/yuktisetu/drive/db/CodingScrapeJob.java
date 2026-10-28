package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * The scrape queue shared with yuktisetu-scraper (Python).
 *
 * Mapped so the schema is declared on the Java side too -- ddl-auto=validate
 * checks it at startup, and the integration tests' create-drop builds it -- but
 * Java never writes it through JPA. drive-service inserts rows with plain SQL
 * inside the apply transaction (it needs ON CONFLICT and FOR UPDATE), and the
 * Python worker owns every transition after that. The partial unique index on
 * pending jobs, the trigger that wakes the worker and the column defaults live
 * only in .schema/2026-10-15-apply-time-coding-capture.sql.
 *
 * Statuses: PENDING, RUNNING, DONE, FAILED. last_status carries the scraper's
 * own verdict (OK, HANDLE_NOT_FOUND, PLATFORM_ERROR, PARSE_FAILED, RATE_LIMITED).
 */
@Entity
@Table(name = "coding_scrape_jobs")
@Getter
@Setter
public class CodingScrapeJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @Column(name = "handle", nullable = false, length = 128)
    private String handle;

    @Column(name = "reason", nullable = false, length = 24)
    private String reason;

    @Column(name = "posting_id")
    private Long postingId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** The absolute limit. Past it, the job fails and its students go to TnP review. */
    @Column(name = "give_up_at", nullable = false)
    private Instant giveUpAt;

    @Column(name = "last_status", length = 24)
    private String lastStatus;

    @Column(name = "snapshot_id")
    private Long snapshotId;
}
