package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Mirrors institute-dal's BaseAuditableEntity, minus createdBy/updatedBy.
 *
 * Duplicated rather than reused on purpose: notification-dal deliberately
 * depends on nothing, so that notification-service keeps delivering mail no
 * matter how the academic model changes. Reusing the original would mean pulling
 * institute-dal -- and with it College and Department -- onto the classpath of a
 * service that never joins to either.
 *
 * There is no createdBy here because notifications are written by a Kafka
 * consumer, not by a person.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class NotificationAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
