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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import org.yuktisetu.model.CriterionOperator;
import org.yuktisetu.model.CriterionSource;
import org.yuktisetu.model.CriterionValueType;
import org.yuktisetu.model.MissingDataPolicy;

/**
 * The dynamic half of the hybrid criteria model: a company one-off like "must
 * hold an AWS certification" or "two publications", added by the TnP with no
 * deployment.
 *
 * source = DYNAMIC_FIELD is the important case -- criterionKey then matches
 * DynamicFieldDefinition.fieldKey in user-dal and resolves through
 * StudentDynamicFieldValue. Company one-offs plug into the Advanced Fields
 * mechanism that already exists rather than growing a parallel store beside it.
 */
@Entity
@Table(
        name = "posting_custom_criteria",
        uniqueConstraints = @UniqueConstraint(name = "uk_custom_criterion", columnNames = {"criteria_id", "criterion_key"}),
        indexes = @Index(name = "idx_custom_criterion_criteria_id", columnList = "criteria_id")
)
@Getter
@Setter
public class PostingCustomCriterion extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criteria_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_custom_criterion_criteria"))
    private PostingEligibilityCriteria criteria;

    /** For DYNAMIC_FIELD, this is DynamicFieldDefinition.fieldKey. */
    @Column(name = "criterion_key", nullable = false, length = 64)
    private String criterionKey;

    @Column(name = "label", nullable = false, length = 128)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 24)
    private CriterionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 12)
    private CriterionOperator operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 16)
    private CriterionValueType valueType;

    /**
     * The comparison operand, JSON-encoded so one column serves scalars, lists
     * and ranges. Plain String + columnDefinition, matching
     * DynamicFieldDefinition.validationRules -- this project has no
     * hypersistence/JsonType dependency and should not grow one for this.
     */
    // @JdbcTypeCode(SqlTypes.JSON) is load-bearing, not decoration: Postgres will
    // not implicitly cast a bound varchar into a jsonb column, so without it every
    // insert fails with "column is of type jsonb but expression is of type
    // character varying". The column stays a String in Java -- the value is
    // already serialized JSON -- and Hibernate binds it as jsonb.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", nullable = false, columnDefinition = "jsonb")
    private String valueJson;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** False = advisory; failing it is recorded but does not block eligibility. */
    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "missing_data_policy", nullable = false, length = 16)
    private MissingDataPolicy missingDataPolicy = MissingDataPolicy.NEEDS_REVIEW;
}
