package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.DriveScoreRun;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriveScoreRunRepository extends JpaRepository<DriveScoreRun, Long> {

    /** The drive's current run. Superseded runs (a reopened drive) are history only. */
    Optional<DriveScoreRun> findByPostingIdAndSupersededAtIsNull(Long postingId);

    List<DriveScoreRun> findByPostingIdIn(Collection<Long> postingIds);

    /**
     * Drives whose gate is READY but which were never scored -- the ready event
     * was lost, or ended FAILED in the outbox. The reconciler scores these from
     * the gate row, so a lost message delays scoring instead of preventing it.
     * Returns [postingId, readyEventId].
     */
    @Query(value = """
            SELECT g.posting_id, g.ready_event_id
              FROM drive_scoring_gates g
             WHERE g.state = 'READY'
               AND NOT EXISTS (SELECT 1 FROM drive_score_runs r
                                WHERE r.posting_id = g.posting_id AND r.superseded_at IS NULL)
             ORDER BY g.posting_id
            """, nativeQuery = true)
    List<Object[]> findReadyWithoutRun();

    @Query("select r.id from DriveScoreRun r where r.postingId = :postingId and r.supersededAt is null")
    Optional<Long> findIdByPostingId(@Param("postingId") Long postingId);
}
