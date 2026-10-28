package org.yuktisetu.drive.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.yuktisetu.drive.model.ScoringFactor;
import org.yuktisetu.drive.model.WeightProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * One drive's composite-scoring configuration, exactly as the TnP entered it.
 *
 * A column per factor rather than a child table: the factor set is a closed enum,
 * and the scorer reads all five as a unit, so a row is the natural shape. There
 * are no meaningful defaults -- 0 is "takes no part" -- and no limits; see
 * WeightProfile for why both are deliberate.
 */
@Entity
@Table(
        name = "posting_scoring_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_scoring_config_posting", columnNames = "posting_id")
)
@Getter
@Setter
public class PostingScoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posting_id", nullable = false)
    private Long postingId;

    @Column(name = "weight_academic", nullable = false, precision = 10, scale = 4)
    private BigDecimal weightAcademic = BigDecimal.ZERO;

    @Column(name = "weight_coding", nullable = false, precision = 10, scale = 4)
    private BigDecimal weightCoding = BigDecimal.ZERO;

    @Column(name = "weight_skill_match", nullable = false, precision = 10, scale = 4)
    private BigDecimal weightSkillMatch = BigDecimal.ZERO;

    @Column(name = "weight_experience", nullable = false, precision = 10, scale = 4)
    private BigDecimal weightExperience = BigDecimal.ZERO;

    @Column(name = "weight_certifications", nullable = false, precision = 10, scale = 4)
    private BigDecimal weightCertifications = BigDecimal.ZERO;

    @Column(name = "threshold", nullable = false, precision = 6, scale = 2)
    private BigDecimal threshold = BigDecimal.ZERO;

    @Column(name = "floor_academic", precision = 6, scale = 2)
    private BigDecimal floorAcademic;

    @Column(name = "floor_coding", precision = 6, scale = 2)
    private BigDecimal floorCoding;

    @Column(name = "floor_skill_match", precision = 6, scale = 2)
    private BigDecimal floorSkillMatch;

    @Column(name = "floor_experience", precision = 6, scale = 2)
    private BigDecimal floorExperience;

    @Column(name = "floor_certifications", precision = 6, scale = 2)
    private BigDecimal floorCertifications;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Every factor with its raw weight, zeros included. */
    public Map<ScoringFactor, BigDecimal> weights() {
        Map<ScoringFactor, BigDecimal> m = new EnumMap<>(ScoringFactor.class);
        m.put(ScoringFactor.ACADEMIC_PERFORMANCE, weightAcademic);
        m.put(ScoringFactor.CODING_PROFICIENCY, weightCoding);
        m.put(ScoringFactor.SKILL_MATCH, weightSkillMatch);
        m.put(ScoringFactor.EXPERIENCE_RELEVANCE, weightExperience);
        m.put(ScoringFactor.CERTIFICATIONS, weightCertifications);
        return m;
    }

    /** Only the floors that are set. */
    public Map<ScoringFactor, BigDecimal> floors() {
        Map<ScoringFactor, BigDecimal> m = new EnumMap<>(ScoringFactor.class);
        putIfSet(m, ScoringFactor.ACADEMIC_PERFORMANCE, floorAcademic);
        putIfSet(m, ScoringFactor.CODING_PROFICIENCY, floorCoding);
        putIfSet(m, ScoringFactor.SKILL_MATCH, floorSkillMatch);
        putIfSet(m, ScoringFactor.EXPERIENCE_RELEVANCE, floorExperience);
        putIfSet(m, ScoringFactor.CERTIFICATIONS, floorCertifications);
        return m;
    }

    public void setWeight(ScoringFactor factor, BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        switch (factor) {
            case ACADEMIC_PERFORMANCE -> weightAcademic = v;
            case CODING_PROFICIENCY -> weightCoding = v;
            case SKILL_MATCH -> weightSkillMatch = v;
            case EXPERIENCE_RELEVANCE -> weightExperience = v;
            case CERTIFICATIONS -> weightCertifications = v;
        }
    }

    public void setFloor(ScoringFactor factor, BigDecimal value) {
        switch (factor) {
            case ACADEMIC_PERFORMANCE -> floorAcademic = value;
            case CODING_PROFICIENCY -> floorCoding = value;
            case SKILL_MATCH -> floorSkillMatch = value;
            case EXPERIENCE_RELEVANCE -> floorExperience = value;
            case CERTIFICATIONS -> floorCertifications = value;
        }
    }

    /**
     * The profile the scorer runs on: only factors weighted above zero.
     *
     * Zero-weighted factors are left out entirely rather than listed as "weighted
     * zero", so a student's breakdown shows what this drive actually measured.
     */
    public WeightProfile toWeightProfile() {
        Map<ScoringFactor, BigDecimal> positive = new EnumMap<>(ScoringFactor.class);
        weights().forEach((f, w) -> {
            if (w != null && w.signum() > 0) {
                positive.put(f, w);
            }
        });
        return WeightProfile.of(positive);
    }

    private static void putIfSet(Map<ScoringFactor, BigDecimal> m, ScoringFactor f, BigDecimal v) {
        if (v != null) {
            m.put(f, v);
        }
    }
}
