package org.yuktisetu.identity.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yuktisetu.identity.model.RoleType;

import java.util.Date;
import jakarta.persistence.ForeignKey;
import org.yuktisetu.institute.db.College;
import org.yuktisetu.institute.db.Degree;
import org.yuktisetu.institute.db.Department;

@Entity
@Table(name = "user_role_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_role_college_dept",
                columnNames = {"user_id", "role", "college_id", "dept_id"}
        ),
        indexes = {
                // The cohort lookup: "every active student in this college and
                // department". Load-bearing, not decorative -- the drive targeting
                // join runs exactly this predicate, and the unique constraint above
                // cannot serve it because that index is led by user_id, so Postgres
                // would sequentially scan the whole assignment table once per
                // posting target. At 100k+ assignments that is the difference
                // between a few milliseconds and several seconds per drive.
                //
                // Column order follows selectivity: role and is_active narrow
                // hardest and are always equality predicates, then college, then
                // department (which the query sometimes leaves unconstrained to
                // mean "all departments").
                @Index(name = "idx_role_assignment_cohort",
                        columnList = "role, is_active, college_id, dept_id"),
                // Reverse direction: "who holds a role in this college", used by
                // the coordinator and HoD listing screens.
                @Index(name = "idx_role_assignment_college_role",
                        columnList = "college_id, role, is_active")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRoleAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoleType role;

    // Scope — nullable on purpose, meaning differs per role:
    // STUDENT / FACULTY_DEPT_COORDINATOR / HOD / GROUND_VOLUNTEER / TNP_COORDINATOR / TNP_COLLEGE_ADMIN -> collegeId required
    // FACULTY_DEPT_COORDINATOR / HOD -> deptId also required
    // TNP_SUPER_ADMIN / IT_ADMIN -> both null (trust-wide)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", foreignKey = @ForeignKey(name = "fk_role_assignment_college"))
    private College college;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", foreignKey = @ForeignKey(name = "fk_role_assignment_department"))
    private Department department;

    // Only meaningful for roles that represent an academic program (STUDENT
    // today) -- e.g. "B.Tech", "M.Tech". Admin-set at role-assignment time
    // (bulk-student-import), never student-editable. Nullable since it does
    // not apply to non-academic roles.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "degree_id", foreignKey = @ForeignKey(name = "fk_role_assignment_degree"))
    private Degree degree;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "assigned_at")
    private Date assignedAt;

    @JoinColumn(name = "assigned_by", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User assignedBy;

    @Column(name = "revoked_at")
    private Date revokedAt;

    @JoinColumn(name = "revoked_by")
    @ManyToOne(fetch = FetchType.LAZY)
    private User revokedBy;

    public Long getCollegeId() { return college != null ? college.getId() : null; }
    public Long getDeptId()    { return department != null ? department.getId() : null; }
}
