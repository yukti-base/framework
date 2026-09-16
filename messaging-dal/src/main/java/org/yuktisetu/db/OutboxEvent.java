package org.yuktisetu.db;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.yuktisetu.model.OutboxStatus;

import java.time.Instant;

/**
 * The transactional outbox. Every service that publishes to Kafka writes here
 * instead of calling KafkaTemplate directly.
 *
 * The problem this solves is not theoretical: publish a drive, commit the status
 * change, then have the Kafka send fail -- and the drive is live while nobody is
 * ever notified, with no record that anything went wrong. Spring's
 * @TransactionalEventListener(AFTER_COMMIT) does not fix it either; it just
 * moves the gap a few milliseconds later.
 *
 * Writing this row inside the SAME transaction as the business change makes the
 * two atomic: either the drive is published and the event is queued, or neither
 * happened. A polling relay then drains PENDING rows to Kafka.
 *
 * The consequence is at-least-once delivery, so every consumer must be
 * idempotent. eventId is the dedupe key consumers key against.
 *
 * Deliberately does NOT extend BaseAuditableEntity: an outbox row is never
 * soft-deleted and has no human author, so those columns would be dead weight --
 * and it would drag an institute-dal dependency into a module that auth-service
 * has to use without ever seeing a College.
 */
@Entity
@Table(
        name = "outbox_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id"),
        indexes = {
                // The relay's only query: claim the next batch of due rows.
                @Index(name = "idx_outbox_status_available", columnList = "status, available_at"),
                @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
        }
)
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** UUID. The consumer-side idempotency key. */
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "aggregate_type", length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 64)
    private String aggregateId;

    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    @Column(name = "event_type", nullable = false, length = 96)
    private String eventType;

    /** Kafka partition key -- keeps per-posting or per-recipient ordering. */
    @Column(name = "partition_key", length = 64)
    private String partitionKey;

    /** The full EventEnvelope, JSON-encoded. */
    // @JdbcTypeCode(SqlTypes.JSON) is load-bearing, not decoration: Postgres will
    // not implicitly cast a bound varchar into a jsonb column, so without it every
    // insert fails with "column is of type jsonb but expression is of type
    // character varying". The column stays a String in Java -- the value is
    // already serialized JSON -- and Hibernate binds it as jsonb.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private String headers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    /** Exponential backoff target. The relay only claims rows whose time has come. */
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
