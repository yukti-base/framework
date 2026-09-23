package org.yuktisetu.institute.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.yuktisetu.institute.db.College;
import org.yuktisetu.institute.model.TenantStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {
    List<College> findByTrustIdAndIsDeletedFalse(Long trustId);

    // Paged variant for the admin "Colleges" listing screen — not that
    // pagination matters at ~10-15 rows, but the screen will eventually
    // also list soft-deleted/inactive ones together, so keep it uniform.
    Page<College> findByTrustIdAndIsDeletedFalse(Long trustId, Pageable pageable);

    Optional<College> findByIdAndIsDeletedFalse(Long id);

    List<College> findByTrustIdAndStatusAndIsDeletedFalse(Long trustId, TenantStatus status);

    boolean existsByTrustIdAndCodeIgnoreCaseAndIsDeletedFalse(Long trustId, String code);

    List<College> findByTrustIdAndIsDeletedTrue(Long trustId); // restore screen

    long countByTrustIdAndIsDeletedFalse(Long trustId); // trust-wide dashboard tile

    boolean existsByTrustId(Long trustId);

    College findByCodeAndIsDeletedFalse(String code);

    /**
     * Every college named by a bulk upload, in ONE query.
     *
     * The single-code finder above forced a per-row lookup, which the fleet's
     * standing rule forbids inside a loop. Callers fetch the whole set up front
     * and index it by code in a HashMap.
     */
    List<College> findByCodeInAndIsDeletedFalse(Collection<String> codes);

}

