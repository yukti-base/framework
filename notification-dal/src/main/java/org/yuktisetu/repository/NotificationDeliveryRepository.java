package org.yuktisetu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.NotificationDelivery;
import org.yuktisetu.model.DeliveryStatus;
import org.yuktisetu.model.NotificationChannel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    Optional<NotificationDelivery> findByIdAndIsDeletedFalse(Long id);

    /** The admin-facing answer to "was this student actually told?". */
    List<NotificationDelivery> findBySourceEventId(String sourceEventId);

    List<NotificationDelivery> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    Page<NotificationDelivery> findByStatusAndIsDeletedFalse(DeliveryStatus status, Pageable pageable);

    Page<NotificationDelivery> findByChannelAndStatusAndIsDeletedFalse(NotificationChannel channel, DeliveryStatus status, Pageable pageable);

    boolean existsBySourceEventIdAndRecipientAddressAndChannel(String sourceEventId, String recipientAddress, NotificationChannel channel);

    /**
     * The retry sweep. Claims rows whose backoff has elapsed, newest attempt
     * first-come, with SKIP LOCKED so more than one instance can drain the
     * queue without serialising on the same row.
     */
    @Query(value = """
            SELECT * FROM notification_deliveries
            WHERE status IN ('PENDING','RETRY')
              AND (next_attempt_at IS NULL OR next_attempt_at <= now())
              AND is_deleted = false
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationDelivery> claimRetryBatch(@Param("batchSize") int batchSize);

    long countByStatusAndCreatedAtAfter(DeliveryStatus status, Instant after);
}
