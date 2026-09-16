package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.BroadcastDispatch;

import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastDispatchRepository extends JpaRepository<BroadcastDispatch, Long> {

    Optional<BroadcastDispatch> findByGroup_IdAndSourceEventId(Long groupId, String sourceEventId);

    boolean existsByGroup_IdAndSourceEventId(Long groupId, String sourceEventId);

    List<BroadcastDispatch> findBySourceEventId(String sourceEventId);
}
