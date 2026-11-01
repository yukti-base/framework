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
import org.yuktisetu.drive.model.RoundChangeKind;
import org.yuktisetu.drive.model.RoundMarkSource;
import org.yuktisetu.drive.model.RoundOutcome;

import java.time.Instant;

/**
 * Append-only: every change to a round result, by whom, from where, and why.
 *
 * The answer to "the volunteer marked me absent but I was there": who marked
 * what, when, and every correction with the coordinator's reason. Draft marks
 * are recorded too, not only published ones.
 */
@Entity
@Table(
        name = "round_result_history",
        indexes = {
                @Index(name = "idx_round_history_round", columnList = "round_id, changed_at"),
                @Index(name = "idx_round_history_application", columnList = "application_id")
        }
)
@Getter
@Setter
public class RoundResultHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /** Null when there was no mark before. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_outcome", length = 16)
    private RoundOutcome fromOutcome;

    /** Null when the mark was removed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_outcome", length = 16)
    private RoundOutcome toOutcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private RoundChangeKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private RoundMarkSource source;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
