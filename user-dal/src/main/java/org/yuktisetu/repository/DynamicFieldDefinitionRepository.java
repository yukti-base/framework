package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yuktisetu.db.DynamicFieldDefinition;

import java.util.List;
import java.util.Optional;

public interface DynamicFieldDefinitionRepository extends JpaRepository<DynamicFieldDefinition, Long> {
    Optional<DynamicFieldDefinition> findByFieldKey(String fieldKey);
    List<DynamicFieldDefinition> findByIsActiveTrue();

    // Cascade guard for hard delete — created_by_user_id is NOT NULL, so
    // removing the creator of a field definition would break the FK.
    long countByCreatedByUser_Id(Long userId);
}
