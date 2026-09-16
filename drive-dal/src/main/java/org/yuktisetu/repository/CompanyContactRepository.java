package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.CompanyContact;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyContactRepository extends JpaRepository<CompanyContact, Long> {

    List<CompanyContact> findByCompany_IdAndIsDeletedFalse(Long companyId);

    Optional<CompanyContact> findByIdAndIsDeletedFalse(Long id);

    Optional<CompanyContact> findByCompany_IdAndIsPrimaryTrueAndIsDeletedFalse(Long companyId);

    boolean existsByCompany_IdAndEmailIgnoreCaseAndIsDeletedFalse(Long companyId, String email);

    // Cascade guard for company hard delete.
    long countByCompany_Id(Long companyId);
}
