package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yuktisetu.db.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByCollegeIdAndIsDeletedFalse(Long collegeId);

    Optional<Department> findByIdAndIsDeletedFalse(Long id);

    boolean existsByCollegeIdAndCodeIgnoreCaseAndIsDeletedFalse(Long collegeId, String code);

    List<Department> findByCollegeIdAndIsDeletedTrue(Long collegeId); // restore screen

    long countByCollegeIdAndIsDeletedFalse(Long collegeId);


    boolean existsByCollegeId(Long collegeId);
    // Bulk fetch for a trust-wide department listing across many colleges
    // in one query — avoids looping per-college in the service layer.
    List<Department> findByCollegeIdInAndIsDeletedFalse(List<Long> collegeIds);
}
