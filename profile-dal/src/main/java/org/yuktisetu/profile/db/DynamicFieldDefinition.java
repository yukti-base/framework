package org.yuktisetu.profile.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.yuktisetu.profile.model.DynamicFieldDataType;

import java.util.Date;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.yuktisetu.identity.db.User;

// DynamicFieldDefinition.java
@Entity
@Table(name = "dynamic_field_definitions")
@Getter
@Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DynamicFieldDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_key", nullable = false, unique = true, updatable = false)
    private String fieldKey;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, updatable = false)
    private DynamicFieldDataType dataType;

    // @JdbcTypeCode(SqlTypes.JSON) is load-bearing, not decoration: Postgres will
    // not implicitly cast a bound varchar into a jsonb column, so without it every
    // insert fails with "column is of type jsonb but expression is of type
    // character varying". The column stays a String in Java -- the value is
    // already serialized JSON -- and Hibernate binds it as jsonb.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private String validationRules;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    private Date updatedAt;
}