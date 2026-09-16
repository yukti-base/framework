package org.yuktisetu.db;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "student_dynamic_field_values",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_profile_id", "field_definition_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentDynamicFieldValue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private DynamicFieldDefinition fieldDefinition;

    private String valueString;
    private BigDecimal valueNumber;
    private Date valueDate;
    private Boolean valueBoolean;

    private Date updatedAt;
}
