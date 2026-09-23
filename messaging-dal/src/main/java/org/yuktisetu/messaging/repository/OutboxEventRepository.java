package org.yuktisetu.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.messaging.db.OutboxEvent;
import org.yuktisetu.messaging.model.OutboxStatus;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Claim a batch for publishing.
     *
     * FOR UPDATE SKIP LOCKED is what makes this safe with more than one service
     * instance running: each relay grabs rows nobody else holds and never blocks
     * waiting for a peer. Without SKIP LOCKED two instances serialise on the same
     * head-of-queue row and the relay throughput collapses to one worker.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING' AND available_at <= now()
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimBatch(@Param("batchSize") int batchSize);

    long countByStatus(OutboxStatus status);

    List<OutboxEvent> findByStatusOrderByIdAsc(OutboxStatus status);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    /** Housekeeping: published rows are kept a week for debugging, then purged. */
    @Modifying
    @Query("delete from OutboxEvent e where e.status = :status and e.publishedAt < :before")
    int purgePublishedBefore(@Param("status") OutboxStatus status, @Param("before") Instant before);
}
