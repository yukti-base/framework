package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.RoundResult;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoundResultRepository extends JpaRepository<RoundResult, Long> {

    List<RoundResult> findByRoundId(Long roundId);

    List<RoundResult> findByRoundIdAndApplicationIdIn(Long roundId, Collection<Long> applicationIds);

    List<RoundResult> findByPostingId(Long postingId);

    /** Any mark at all on the drive -- once there is one, the drive cannot be reopened. */
    boolean existsByPostingId(Long postingId);

    /**
     * One student's PUBLISHED results on these drives, for their own drive pages.
     * Draft marks on a round still PLANNED are never shown to a student.
     */
    @Query("""
            select rr from RoundResult rr, DriveRound r
             where r.id = rr.roundId
               and rr.studentUserId = :studentUserId
               and rr.postingId in :postingIds
               and r.status = org.yuktisetu.drive.model.RoundStatus.PUBLISHED
            """)
    List<RoundResult> findPublishedForStudent(@Param("studentUserId") Long studentUserId,
                                              @Param("postingIds") Collection<Long> postingIds);
}
