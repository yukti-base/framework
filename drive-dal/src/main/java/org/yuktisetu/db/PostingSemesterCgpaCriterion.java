package org.yuktisetu.db;

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

import java.math.BigDecimal;

/**
 * Per-semester GPA floors. Multi-valued, so it cannot live on the 1:1 criteria
 * row. Evaluated against StudentProfile.sem1Gpa..sem8Gpa, mapped by number.
 */
@Entity
@Table(
        name = "posting_semester_cgpa_criteria",
        uniqueConstraints = @UniqueConstraint(name = "uk_semester_criterion", columnNames = {"criteria_id", "semester"}),
        indexes = @Index(name = "idx_semester_criterion_criteria_id", columnList = "criteria_id")
)
@Getter
@Setter
public class PostingSemesterCgpaCriterion extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criteria_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_semester_criterion_criteria"))
    private PostingEligibilityCriteria criteria;

    /** 1-8, matching the sem1Gpa..sem8Gpa columns on StudentProfile. */
    @Column(name = "semester", nullable = false)
    private Short semester;

    @Column(name = "min_gpa", nullable = false, precision = 4, scale = 2)
    private BigDecimal minGpa;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
