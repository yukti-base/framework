package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.ApplicationScore;

import java.util.List;

@Repository
public interface ApplicationScoreRepository extends JpaRepository<ApplicationScore, Long> {

    List<ApplicationScore> findByPostingIdOrderByRankAscStudentUserIdAsc(Long postingId);

    /** One run's rows -- the current ranking is the active run's, not the posting's. */
    List<ApplicationScore> findByRunIdOrderByRankAscStudentUserIdAsc(Long runId);

    List<ApplicationScore> findByStudentUserId(Long studentUserId);

    long countByPostingId(Long postingId);

    /** A held run redone after approval replaces its scores wholesale. */
    @Modifying(flushAutomatically = true)
    @Query("delete from ApplicationScore s where s.runId = :runId")
    int deleteByRunId(@Param("runId") Long runId);
}
