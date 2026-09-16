package org.yuktisetu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.Notification;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndIsReadFalseAndIsDeletedFalse(Long recipientUserId);

    Optional<Notification> findByIdAndIsDeletedFalse(Long id);

    /** Dedupe check for a redelivered Kafka message, before relying on the constraint. */
    boolean existsBySourceEventIdAndRecipientUserId(String sourceEventId, Long recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
               set n.isRead = true, n.readAt = :readAt
             where n.recipientUserId = :recipientUserId
               and n.isRead = false
               and n.isDeleted = false
            """)
    int markAllRead(@Param("recipientUserId") Long recipientUserId, @Param("readAt") Instant readAt);
}
