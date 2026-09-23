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
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.drive.model.ActorKind;
import org.yuktisetu.drive.model.ApplicationStatus;

import java.time.Instant;
import org.yuktisetu.persistence.BaseAuditableEntity;

/**
 * Append-only trail of every status change on an application.
 *
 * This exists because application status is the most disputable state in the
 * system: a student who screenshots "Shortlisted" and later sees something else
 * will escalate to the TnP, and without this table the TnP has no answer. Rows
 * are never updated or deleted.
 */
@Entity
@Table(
        name = "application_status_history",
        indexes = {
                @Index(name = "idx_app_status_history_application_id", columnList = "application_id"),
                @Index(name = "idx_app_status_history_changed_at", columnList = "changed_at")
        }
)
@Getter
@Setter
public class ApplicationStatusHistory extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_app_status_history_application"))
    private JobApplication application;

    /** Null on the first row -- the application did not exist before. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /** Null when actorKind is SYSTEM (the deadline sweep). */
    @Column(name = "changed_by")
    private Long changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_kind", nullable = false, length = 12)
    private ActorKind actorKind;

    @Column(name = "reason", length = 255)
    private String reason;
}
