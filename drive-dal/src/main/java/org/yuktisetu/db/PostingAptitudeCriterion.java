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
import org.yuktisetu.model.AptitudeExam;

import java.math.BigDecimal;

/**
 * Aptitude-exam floors beyond CoCubes. Configurable today, but only COCUBES is
 * actually evaluable -- it is the one exam with a column on StudentProfile. The
 * evaluator records the others as UNEVALUATED rather than silently passing them,
 * so a criterion nobody can check never quietly waves a student through.
 */
@Entity
@Table(
        name = "posting_aptitude_criteria",
        uniqueConstraints = @UniqueConstraint(name = "uk_aptitude_criterion", columnNames = {"criteria_id", "exam"}),
        indexes = @Index(name = "idx_aptitude_criterion_criteria_id", columnList = "criteria_id")
)
@Getter
@Setter
public class PostingAptitudeCriterion extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criteria_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_aptitude_criterion_criteria"))
    private PostingEligibilityCriteria criteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam", nullable = false, length = 24)
    private AptitudeExam exam;

    @Column(name = "min_score", nullable = false, precision = 7, scale = 2)
    private BigDecimal minScore;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
