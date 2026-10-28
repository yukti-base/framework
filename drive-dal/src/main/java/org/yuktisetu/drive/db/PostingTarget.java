package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.persistence.BaseAuditableEntity;
import org.yuktisetu.institute.db.College;
import org.yuktisetu.institute.db.Degree;
import org.yuktisetu.institute.db.Department;

/**
 * Who a drive is for: one row per (college, department, batch, degree) the
 * posting is open to. "Infosys, 2027 batch, CS at PCCOE" is one row; adding
 * IT at PCCOE and CS at Akurdi is two more rows on the SAME posting -- one JD,
 * one deadline, one applicant pool.
 *
 * College lives here rather than as a column on the posting precisely so that
 * spanning colleges later needs no migration.
 *
 * Nullable department/degree mean "all of them" within that college.
 */
@Entity
@Table(
        name = "posting_targets",
        uniqueConstraints = @UniqueConstraint(name = "uk_posting_target_key", columnNames = {"posting_id", "target_key"}),
        indexes = {
                @Index(name = "idx_posting_target_posting_id", columnList = "posting_id"),
                // Reverse lookup: given a student's college/department/batch,
                // which postings target them. Load-bearing for the student list.
                @Index(name = "idx_posting_target_lookup", columnList = "college_id, department_id, batch_year")
        }
)
@Getter
@Setter
public class PostingTarget extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_posting_target_posting"))
    private JobPosting posting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_posting_target_college"))
    private College college;

    /** Null = every department in that college. */
    /** Null = every department in that college. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id",
            foreignKey = @ForeignKey(name = "fk_posting_target_department"))
    private Department department;

    /** Matches StudentProfile.graduationYear. */
    @Column(name = "batch_year", nullable = false)
    private Integer batchYear;

    /** Matches UserRoleAssignment.degree. Null = any degree. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "degree_id",
            foreignKey = @ForeignKey(name = "fk_posting_target_degree"))
    private Degree degree;

    /**
     * Service-derived: "c:{collegeId}|d:{deptId|*}|b:{year}|g:{degree|*}".
     *
     * A plain unique constraint over the nullable columns would NOT prevent
     * duplicates: Postgres treats NULL as distinct from NULL in a unique index,
     * so two identical "all departments" rows would both be accepted. Collapsing
     * the tuple into one non-null string makes the constraint actually hold.
     */
    @Column(name = "target_key", nullable = false, length = 128)
    private String targetKey;

    public Long getCollegeId() { return college != null ? college.getId() : null; }
    public Long getDegreeId() { return degree != null ? degree.getId() : null; }
    public Long getDepartmentId() { return department != null ? department.getId() : null; }
}
