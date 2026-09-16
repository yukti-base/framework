package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.model.NotificationSeverity;
import org.yuktisetu.model.NotificationType;

import java.time.Instant;

/**
 * One in-app/portal notification for one recipient -- the bell icon.
 *
 * recipientUserId is a bare Long: this module depends on nothing, and recipient
 * identity arrives denormalised on the dispatch event.
 */
@Entity
@Table(
        name = "notifications",
        uniqueConstraints = {
                // Kafka delivery is at-least-once, so the same dispatch WILL
                // occasionally be consumed twice. This constraint turns the
                // duplicate into a no-op insert instead of a second copy in the
                // student's bell -- idempotency enforced by the database rather
                // than by hoping the consumer never redelivers.
                @UniqueConstraint(name = "uk_notification_event_recipient", columnNames = {"source_event_id", "recipient_user_id"})
        },
        indexes = {
                @Index(name = "idx_notification_recipient_unread", columnList = "recipient_user_id, is_read, created_at")
        }
)
@Getter
@Setter
public class Notification extends NotificationAuditableEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 12)
    private NotificationSeverity severity = NotificationSeverity.INFO;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "action_url", length = 512)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    /** The originating EventEnvelope.eventId. Dedupe key, see the constraint above. */
    @Column(name = "source_event_id", nullable = false, length = 36)
    private String sourceEventId;

    @Column(name = "source_aggregate_type", length = 64)
    private String sourceAggregateType;

    @Column(name = "source_aggregate_id", length = 64)
    private String sourceAggregateId;

    // @JdbcTypeCode(SqlTypes.JSON) is load-bearing, not decoration: Postgres will
    // not implicitly cast a bound varchar into a jsonb column, so without it every
    // insert fails with "column is of type jsonb but expression is of type
    // character varying". The column stays a String in Java -- the value is
    // already serialized JSON -- and Hibernate binds it as jsonb.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
