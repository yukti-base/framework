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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.yuktisetu.drive.model.RoundStatus;
import org.yuktisetu.drive.model.RoundType;

import java.time.Instant;

/**
 * One step of a drive after the shortlist: an aptitude test, an interview, ...
 *
 * Rounds run in seq order, one at a time. The drive's CURRENT round is the
 * lowest-seq round still PLANNED, and only it can be marked. Its candidates are
 * not stored: they are exactly the drive's applications still SHORTLISTED,
 * because publishing a round takes everyone who did not advance out
 * (RoundRules).
 *
 * Two counters guard two different races:
 *  - version (JPA) protects the round's own details -- name, schedule, venue --
 *    from two coordinators editing them at once;
 *  - marksVersion is bumped by every mark on the round, in the same statement
 *    that checks the round is still PLANNED. Publishing must name the
 *    marksVersion the coordinator reviewed, so a publish can never go out over
 *    marks that changed after they looked.
 */
@Entity
@Table(
        name = "drive_rounds",
        uniqueConstraints = @UniqueConstraint(name = "uk_drive_round_seq", columnNames = {"posting_id", "seq"}),
        indexes = @Index(name = "idx_drive_round_posting_status", columnList = "posting_id, status")
)
@Getter
@Setter
public class DriveRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    /** 1-based position in the drive. Renumbered when a planned round is removed. */
    @Column(name = "seq", nullable = false)
    private int seq;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 32)
    private RoundType roundType;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "venue", length = 255)
    private String venue;

    /** Shown to the round's candidates: what to bring, the test link's arrival, ... */
    @Column(name = "instructions", columnDefinition = "text")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RoundStatus status = RoundStatus.PLANNED;

    @Column(name = "marks_version", nullable = false)
    private long marksVersion = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    /** True on the round that was published as the drive's final one. */
    @Column(name = "is_final", nullable = false)
    private boolean finalRound = false;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
