package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.RoundResultHistory;

import java.util.List;

@Repository
public interface RoundResultHistoryRepository extends JpaRepository<RoundResultHistory, Long> {

    List<RoundResultHistory> findByRoundIdAndApplicationIdOrderByChangedAtAscIdAsc(Long roundId, Long applicationId);
}
