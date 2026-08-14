package org.yuktisetu.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.model.TenantStatus;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = @UniqueConstraint(name = "uk_department_college_code", columnNames = {"college_id", "code"}),
        indexes = @Index(name = "idx_department_college_id", columnList = "college_id")
)
@Getter
@Setter
public class Department extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false, foreignKey = @ForeignKey(name = "fk_department_college"))
    private College college;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // e.g. "CS", "IT", "MECH" — scoped unique per college, not globally.
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TenantStatus status = TenantStatus.ACTIVE;

    // Deliberately NOT here: hodUserId, coordinatorUserId. That linkage
    // already exists as UserRoleAssignment(deptId) rows in user-dal —
    // see flaw #1. Duplicating it here creates a second source of truth.
}
