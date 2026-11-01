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
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.drive.model.RoundMarkSource;
import org.yuktisetu.drive.model.RoundOutcome;

import java.time.Instant;

/**
 * One candidate's result in one round. A draft while the round is PLANNED, the
 * student's result once it is PUBLISHED.
 *
 * Written only by conditional SQL upserts (RoundResultRepository), never by
 * entity saves: two volunteers marking the same student at the same moment must
 * not both win. version is the compare-and-set token -- a mark names the version
 * it saw, and a mismatch is reported back as a conflict with the current value
 * instead of silently overwriting it.
 */
@Entity
@Table(
        name = "round_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_round_result_application", columnNames = {"round_id", "application_id"}),
        indexes = {
                @Index(name = "idx_round_result_posting", columnList = "posting_id"),
                @Index(name = "idx_round_result_application", columnList = "application_id")
        }
)
@Getter
@Setter
public class RoundResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private RoundOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private RoundMarkSource source;

    @Column(name = "marked_by", nullable = false)
    private Long markedBy;

    @Column(name = "marked_at", nullable = false)
    private Instant markedAt;

    @Column(name = "version", nullable = false)
    private int version;
}
