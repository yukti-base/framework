package org.yuktisetu.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.identity.db.UserRoleAssignment;
import org.yuktisetu.identity.model.RoleType;

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

    boolean existsByUserIdAndRoleAndDepartmentIdAndIsActiveTrue(Long userId, RoleType role, Long deptId);
    UserRoleAssignment findByUserIdAndRoleAndCollegeIdAndDepartmentIdAndIsActiveTrue(Long userId, RoleType role, Long collegeId, Long deptId);

    List<UserRoleAssignment> findByCollege_IdAndIsActiveTrue(Long collegeId);
    List<UserRoleAssignment> findByDepartment_IdAndIsActiveTrue(Long deptId);
    List<UserRoleAssignment> findByCollege_IdAndRoleAndIsActiveTrue(Long collegeId, RoleType role);

    // ------------------------------------------------------------------
    // Trust-wide (college_id IS NULL, dept_id IS NULL) lookups.
    //
    // These MUST be explicit @Query. The derived-query equivalents above
    // (…AndCollegeIdAndDepartmentId…) bind a null argument as "= NULL",
    // which is never true in SQL, so calling them for a TNP_SUPER_ADMIN or
    // IT_ADMIN silently found nothing and made those two roles impossible
    // to deactivate. Null scope means IS NULL here, spelled out.
    // ------------------------------------------------------------------

    @Query("""
            select a from UserRoleAssignment a
            where a.user.id = :userId
              and a.role = :role
              and a.isActive = true
              and a.user.isDeleted = false
              and a.college is null
              and a.department is null
            """)
    Optional<UserRoleAssignment> findActiveTrustWideAssignment(@Param("userId") Long userId,
                                                               @Param("role") RoleType role);

    @Query("""
            select count(a) from UserRoleAssignment a
            where a.role = :role
              and a.isActive = true
              and a.user.isDeleted = false
              and a.college is null
              and a.department is null
            """)
    long countActiveTrustWideHolders(@Param("role") RoleType role);

    // ------------------------------------------------------------------
    // Actor-authority lookups.
    //
    // Deliberately stricter than the plain isActive checks above: an actor
    // may only act from an assignment that is active AND belongs to an
    // onboarded, non-deleted, ACTIVE account. A PENDING_ACTIVATION invitee,
    // a SUSPENDED account, or a soft-deleted user must never resolve to a
    // usable authority, even while the assignment row itself is still
    // flagged active.
    //
    // Note the asymmetry with the target-side lookups: when we are looking
    // up the person being ACTED ON (deactivate/restore screens), user status
    // is intentionally NOT filtered — a College Admin has to be able to see
    // and reactivate an inactive person under their own ownership.
    // ------------------------------------------------------------------

    @Query("""
            select a from UserRoleAssignment a
            where a.user.id = :userId
              and a.role = :role
              and a.isActive = true
              and a.user.isDeleted = false
              and a.user.status = org.yuktisetu.identity.model.UserStatus.ACTIVE
              and ((:collegeId is null and a.college is null)
                or (:collegeId is not null and a.college.id = :collegeId))
              and ((:deptId is null and a.department is null)
                or (:deptId is not null and a.department.id = :deptId))
            """)
    Optional<UserRoleAssignment> findLiveActorAssignment(@Param("userId") Long userId,
                                                         @Param("role") RoleType role,
                                                         @Param("collegeId") Long collegeId,
                                                         @Param("deptId") Long deptId);

    // Target-side lookup for deactivation: scope-null-safe, and deliberately
    // does not filter on user status (see note above).
    @Query("""
            select a from UserRoleAssignment a
            where a.user.id = :userId
              and a.role = :role
              and a.isActive = true
              and a.user.isDeleted = false
              and ((:collegeId is null and a.college is null)
                or (:collegeId is not null and a.college.id = :collegeId))
              and ((:deptId is null and a.department is null)
                or (:deptId is not null and a.department.id = :deptId))
            """)
    Optional<UserRoleAssignment> findActiveAssignmentInScope(@Param("userId") Long userId,
                                                             @Param("role") RoleType role,
                                                             @Param("collegeId") Long collegeId,
                                                             @Param("deptId") Long deptId);

    // ------------------------------------------------------------------
    // Cascade guards for genuine hard delete. Every assignment row, active
    // or revoked, plus every row that merely REFERENCES this user as the
    // actor who assigned or revoked someone else's role. User.roleAssignments
    // is mapped CascadeType.ALL, so deleting the row without these checks
    // would quietly take real audit history with it.
    // ------------------------------------------------------------------

    long countByUser_Id(Long userId);

    // Every assignment this person has ever held, active or revoked. Used by
    // soft delete, which must check the actor's authority over each historical
    // role at that role's own scope — not just over whatever is active today.
    List<UserRoleAssignment> findAllByUser_Id(Long userId);

    long countByAssignedBy_Id(Long userId);

    long countByRevokedBy_Id(Long userId);
}
