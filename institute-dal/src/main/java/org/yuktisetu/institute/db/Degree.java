package org.yuktisetu.institute.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.institute.model.TenantStatus;
import org.yuktisetu.persistence.BaseAuditableEntity;

/**
 * An academic programme: B.Tech, M.Tech, MCA, BCA, MBA, Diploma.
 *
 * A trust-wide catalogue, not per college. Which colleges offer a degree is
 * College.degrees; which degree a department belongs to is Department.degree
 * (exactly one -- "Computer Engineering (B.Tech)" and "Computer Engineering
 * (M.Tech)" are two departments with two codes).
 *
 * This is what a student's degree and a drive target's degree point at, instead
 * of the free-text "B.Tech" / "btech" / "B Tech" strings that used to be matched
 * with =.
 */
@Entity
@Table(
        name = "degrees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_degree_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_degree_name", columnNames = "name")
        }
)
@Getter
@Setter
public class Degree extends BaseAuditableEntity {

    /** Stable machine handle, upper-case: BTECH, MTECH, MCA. */
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    /** What people write and read: "B.Tech". */
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TenantStatus status = TenantStatus.ACTIVE;
}
