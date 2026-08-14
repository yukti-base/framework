package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yuktisetu.model.RoleType;

import java.util.Date;

@Entity
@Table(name = "user_role_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_role_college_dept",
                columnNames = {"user_id", "role", "college_id", "dept_id"}
        ))
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
    private College college;     // FK -> org-dal College.id, no hard FK across modules — validated at service layer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", foreignKey = @ForeignKey(name = "fk_role_assignment_department"))
    private Department department;      // FK -> org-dal Department.id

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
