package org.yuktisetu.drive.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.DriveRound;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriveRoundRepository extends JpaRepository<DriveRound, Long> {

    List<DriveRound> findByPostingIdOrderBySeqAsc(Long postingId);

    List<DriveRound> findByPostingIdInOrderByPostingIdAscSeqAsc(Collection<Long> postingIds);

    /**
     * The round, write-locked. Every path that marks, publishes, corrects or
     * edits a round takes this lock after the posting's, in that order, so two of
     * them can never interleave and can never deadlock on each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from DriveRound r where r.id = :id")
    Optional<DriveRound> lockById(@Param("id") Long id);

    long countByPostingId(Long postingId);
}
