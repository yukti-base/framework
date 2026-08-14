package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yuktisetu.db.Trust;

import java.util.List;
import java.util.Optional;

public interface TrustRepository extends JpaRepository<Trust, Long> {
    List<Trust> findByIsDeletedFalse();

    Optional<Trust> findByIdAndIsDeletedFalse(Long id);

    boolean existsByCodeIgnoreCaseAndIsDeletedFalse(String code);

    // For the "restore soft-deleted" admin screen
    List<Trust> findByIsDeletedTrue();
}
