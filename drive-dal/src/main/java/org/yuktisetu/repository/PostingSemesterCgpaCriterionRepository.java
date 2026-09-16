package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.PostingSemesterCgpaCriterion;

import java.util.List;

@Repository
public interface PostingSemesterCgpaCriterionRepository extends JpaRepository<PostingSemesterCgpaCriterion, Long> {

    List<PostingSemesterCgpaCriterion> findByCriteria_IdAndEnabledTrueAndIsDeletedFalse(Long criteriaId);

    List<PostingSemesterCgpaCriterion> findByCriteria_IdAndIsDeletedFalse(Long criteriaId);

    void deleteByCriteria_Id(Long criteriaId);
}
