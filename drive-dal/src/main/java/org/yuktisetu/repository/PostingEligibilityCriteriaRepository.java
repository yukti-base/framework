package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.PostingEligibilityCriteria;

import java.util.Optional;

@Repository
public interface PostingEligibilityCriteriaRepository extends JpaRepository<PostingEligibilityCriteria, Long> {

    Optional<PostingEligibilityCriteria> findByPosting_IdAndIsDeletedFalse(Long postingId);

    boolean existsByPosting_IdAndIsDeletedFalse(Long postingId);
}
