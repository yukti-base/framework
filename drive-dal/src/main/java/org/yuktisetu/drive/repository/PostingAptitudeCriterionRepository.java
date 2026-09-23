package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.PostingAptitudeCriterion;

import java.util.List;

@Repository
public interface PostingAptitudeCriterionRepository extends JpaRepository<PostingAptitudeCriterion, Long> {

    List<PostingAptitudeCriterion> findByCriteria_IdAndEnabledTrueAndIsDeletedFalse(Long criteriaId);

    List<PostingAptitudeCriterion> findByCriteria_IdAndIsDeletedFalse(Long criteriaId);

    void deleteByCriteria_Id(Long criteriaId);
}
