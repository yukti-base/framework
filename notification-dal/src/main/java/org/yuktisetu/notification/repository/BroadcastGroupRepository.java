package org.yuktisetu.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.notification.db.BroadcastGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastGroupRepository extends JpaRepository<BroadcastGroup, Long> {

    Optional<BroadcastGroup> findByAliasEmailIgnoreCaseAndIsDeletedFalse(String aliasEmail);

    Optional<BroadcastGroup> findByIdAndIsDeletedFalse(Long id);

    List<BroadcastGroup> findByIsActiveTrueAndIsDeletedFalse();

    /** Resolve the alias for a drive's audience: this college, this dept, this batch. */
    List<BroadcastGroup> findByCollegeIdAndIsActiveTrueAndIsDeletedFalse(Long collegeId);
}
