package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.drive.model.PlacedStudentDecision;

import java.time.Instant;
import org.yuktisetu.persistence.BaseAuditableEntity;
import org.yuktisetu.identity.db.User;

/**
 * The TnP admin's explicit ruling on ONE already-placed student for ONE drive.
 *
 * This is the final word in the eligibility chain: hard gates, then the
 * internship lock, then the uplift floor, then this row -- which overrides
 * everything above it in BOTH directions. A DENY blocks a student who would
 * otherwise clear the uplift; an ALLOW admits one who would not.
 *
 * reason is NOT NULL on purpose. This is the decision a student is most likely
 * to dispute, and "because the admin said so" with no recorded rationale is how
 * the TnP loses that argument.
 */
@Entity
@Table(
        name = "drive_placed_student_decisions",
        uniqueConstraints = @UniqueConstraint(name = "uk_drive_placed_decision", columnNames = {"posting_id", "student_user_id"}),
        indexes = {
                @Index(name = "idx_placed_decision_posting_id", columnList = "posting_id"),
                @Index(name = "idx_placed_decision_student", columnList = "student_user_id")
        }
)
@Getter
@Setter
public class DrivePlacedStudentDecision extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_placed_decision_posting"))
    private JobPosting posting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_placed_decision_student"))
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 8)
    private PlacedStudentDecision decision;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Column(name = "decided_by", nullable = false)
    private Long decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    public Long getStudentUserId() { return student != null ? student.getId() : null; }
}
