package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.EligibilityRun;

import java.util.List;
import java.util.Optional;

@Repository
public interface EligibilityRunRepository extends JpaRepository<EligibilityRun, Long> {

    List<EligibilityRun> findByPosting_IdOrderByStartedAtDesc(Long postingId);

    Optional<EligibilityRun> findFirstByPosting_IdOrderByStartedAtDesc(Long postingId);
}
