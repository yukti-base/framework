package org.yuktisetu.drive.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.DriveScoringGate;
import org.yuktisetu.drive.model.ScoringGateState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriveScoringGateRepository extends JpaRepository<DriveScoringGate, Long> {

    Optional<DriveScoringGate> findByPostingId(Long postingId);

    /**
     * The gate row, locked. Always the FIRST lock a settling transaction takes.
     *
     * A bounded wait: if something holds the gate for longer than ten seconds the
     * caller fails and is retried (Kafka redelivery, the next reconciler pass)
     * rather than queueing behind it indefinitely.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000"))
    @Query("select g from DriveScoringGate g where g.postingId = :postingId")
    Optional<DriveScoringGate> lockByPostingId(@Param("postingId") Long postingId);

    /** Posting ids of gates not yet finished, for the safety-net reconciler. */
    @Query("select g.postingId from DriveScoringGate g where g.state in :states order by g.postingId")
    List<Long> findPostingIdsByStateIn(@Param("states") Collection<ScoringGateState> states);
}
