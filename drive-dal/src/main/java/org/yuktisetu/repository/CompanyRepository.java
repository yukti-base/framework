package org.yuktisetu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.Company;
import org.yuktisetu.model.CompanyStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // Soft delete is expressed here as a query suffix, never as @SQLRestriction,
    // matching institute-dal. Callers that legitimately need deleted rows (the
    // restore screen) use the explicit ...IsDeletedTrue query below.
    Optional<Company> findByIdAndIsDeletedFalse(Long id);

    Optional<Company> findByCodeIgnoreCaseAndIsDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndIsDeletedFalse(String code);

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    Page<Company> findByIsDeletedFalse(Pageable pageable);

    Page<Company> findByStatusAndIsDeletedFalse(CompanyStatus status, Pageable pageable);

    List<Company> findByIsDeletedTrue();
}
