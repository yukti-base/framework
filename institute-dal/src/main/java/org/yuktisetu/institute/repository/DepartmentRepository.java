package org.yuktisetu.institute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.institute.db.Department;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByCollegeIdAndIsDeletedFalse(Long collegeId);

    Optional<Department> findByIdAndIsDeletedFalse(Long id);

    boolean existsByCollegeIdAndCodeIgnoreCaseAndIsDeletedFalse(Long collegeId, String code);

    List<Department> findByCollegeIdAndIsDeletedTrue(Long collegeId); // restore screen

    long countByCollegeIdAndIsDeletedFalse(Long collegeId);

    Department findByCodeAndIsDeletedFalse(String code);

    /** Department counterpart of CollegeRepository.findByCodeInAndIsDeletedFalse. */
    List<Department> findByCodeInAndIsDeletedFalse(Collection<String> codes);


    boolean existsByCollegeId(Long collegeId);
    // Bulk fetch for a trust-wide department listing across many colleges
    // in one query — avoids looping per-college in the service layer.
    List<Department> findByCollegeIdInAndIsDeletedFalse(List<Long> collegeIds);
}
