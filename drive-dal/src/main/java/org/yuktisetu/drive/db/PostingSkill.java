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

/**
 * A table rather than two @ElementCollections of strings, because the
 * required/preferred distinction is data the Skill Match factor will need to
 * weight against -- the SSOT is explicit that required skills count for more.
 */
@Entity
@Table(
        name = "posting_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_posting_skill", columnNames = {"posting_id", "skill"}),
        indexes = @Index(name = "idx_posting_skill_posting_id", columnList = "posting_id")
)
@Getter
@Setter
public class PostingSkill extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_posting_skill_posting"))
    private JobPosting posting;

    @Column(name = "skill", nullable = false, length = 64)
    private String skill;

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory = false;
}
