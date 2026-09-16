package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yuktisetu.db.StudentDynamicFieldValue;

import java.util.List;
import java.util.Optional;

public interface StudentDynamicFieldValueRepository extends JpaRepository<StudentDynamicFieldValue, Long> {
    List<StudentDynamicFieldValue> findByStudentProfile_Id(Long studentProfileId);
    Optional<StudentDynamicFieldValue> findByStudentProfile_IdAndFieldDefinition_Id(Long studentProfileId, Long fieldDefinitionId);
}
