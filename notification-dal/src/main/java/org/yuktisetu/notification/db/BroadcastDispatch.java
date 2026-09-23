package org.yuktisetu.notification.db;

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
import org.yuktisetu.notification.model.DeliveryStatus;

import java.time.Instant;

/** One send to one group alias, deduped per source event like every other delivery. */
@Entity
@Table(
        name = "broadcast_dispatches",
        uniqueConstraints = @UniqueConstraint(name = "uk_broadcast_dispatch", columnNames = {"group_id", "source_event_id"}),
        indexes = @Index(name = "idx_broadcast_dispatch_group_id", columnList = "group_id")
)
@Getter
@Setter
public class BroadcastDispatch extends NotificationAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_broadcast_dispatch_group"))
    private BroadcastGroup group;

    @Column(name = "source_event_id", nullable = false, length = 36)
    private String sourceEventId;

    @Column(name = "subject", length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
