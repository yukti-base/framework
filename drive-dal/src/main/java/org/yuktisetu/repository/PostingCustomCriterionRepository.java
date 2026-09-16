package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.PostingCustomCriterion;

import java.util.List;

@Repository
public interface PostingCustomCriterionRepository extends JpaRepository<PostingCustomCriterion, Long> {

    List<PostingCustomCriterion> findByCriteria_IdAndEnabledTrueAndIsDeletedFalse(Long criteriaId);

    List<PostingCustomCriterion> findByCriteria_IdAndIsDeletedFalse(Long criteriaId);

    boolean existsByCriteria_IdAndCriterionKeyAndIsDeletedFalse(Long criteriaId, String criterionKey);

    void deleteByCriteria_Id(Long criteriaId);
}
