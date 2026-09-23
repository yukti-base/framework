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
import org.yuktisetu.notification.model.DeliveryProvider;
import org.yuktisetu.notification.model.DeliveryStatus;
import org.yuktisetu.notification.model.NotificationChannel;

import java.time.Instant;

/**
 * One row per recipient PER CHANNEL, with its own status and error.
 *
 * This is the table that answers "I never got told about the drive". A missed
 * "you are shortlisted, interview at 9 AM" is a reputation-level failure for the
 * TnP, and without a per-recipient record the only available answer is a shrug.
 * Aggregate send counts are not good enough -- the dispute is always about one
 * specific student.
 */
@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = {
                // Same at-least-once protection as Notification: a redelivered
                // dispatch must not send the same person a second email.
                @UniqueConstraint(name = "uk_delivery_event_recipient_channel",
                        columnNames = {"source_event_id", "recipient_address", "channel"})
        },
        indexes = {
                // The retry sweep's query.
                @Index(name = "idx_delivery_status_next_attempt", columnList = "status, next_attempt_at"),
                @Index(name = "idx_delivery_recipient_user_id", columnList = "recipient_user_id"),
                @Index(name = "idx_delivery_source_event_id", columnList = "source_event_id")
        }
)
@Getter
@Setter
public class NotificationDelivery extends NotificationAuditableEntity {

    /** Null for a pure email/group send with no in-app counterpart. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id",
            foreignKey = @ForeignKey(name = "fk_delivery_notification"))
    private Notification notification;

    /** Null when the recipient is a group alias rather than a person. */
    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    /** Email address or group alias -- whatever the channel actually addressed. */
    @Column(name = "recipient_address", nullable = false, length = 320)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 16)
    private DeliveryProvider provider;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "source_event_id", nullable = false, length = 36)
    private String sourceEventId;

    @Column(name = "template_key", length = 96)
    private String templateKey;
}
