package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.UserRoleAssignment;
import org.yuktisetu.model.RoleType;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long> {
    List<UserRoleAssignment> findByUserIdAndIsActiveTrue(Long userId);

    // scope containment check for the actor creating/deactivating someone
    boolean existsByUserIdAndRoleAndCollegeIdAndIsActiveTrue(Long userId, RoleType role, Long collegeId);

    // trust-wide actor check (Super Admin / IT Admin)
    boolean existsByUserIdAndRoleAndIsActiveTrue(Long userId, RoleType role);

    // "how many active holders of this role remain in this college" (college-scoped roles)
    long countByRoleAndCollegeIdAndIsActiveTrue(RoleType role, Long collegeId);

    // same, but for dept-scoped roles where dept also matters
    long countByRoleAndCollegeIdAndDepartmentIdAndIsActiveTrue(RoleType role, Long collegeId, Long deptId);

    // trust-wide count (Super Admin / IT Admin)
    long countByRoleAndIsActiveTrue(RoleType role);

    // "does any active subordinate exist in this scope" — used by the last-holder guard
    boolean existsByRoleAndCollegeIdAndIsActiveTrue(RoleType role, Long collegeId);
    boolean existsByRoleAndCollegeIdAndDepartmentIdAndIsActiveTrue(RoleType role, Long collegeId, Long deptId);

    // duplicate-assignment prevention (add as a DB unique index too, see note below)
    boolean existsByUserIdAndRoleAndCollegeIdAndDepartmentId(Long userId, RoleType role, Long collegeId, Long deptId);

    List<UserRoleAssignment> findByRoleAndCollegeIdInAndIsActiveTrue(RoleType role, List<Long> collegeIds);

    Optional<UserRoleAssignment> findByIdAndIsActiveTrue(Long id);

    UserRoleAssignment findByUserIdAndRoleAndCollegeIdAndDepartmentIdAndIsActiveTrue(Long userId, RoleType role, Long collegeId, Long deptId);

    List<UserRoleAssignment> findByCollege_IdAndIsActiveTrue(Long collegeId);
    List<UserRoleAssignment> findByDepartment_IdAndIsActiveTrue(Long deptId);
    List<UserRoleAssignment> findByCollege_IdAndRoleAndIsActiveTrue(Long collegeId, RoleType role);
}
