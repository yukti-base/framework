package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.ApplicationCodingReview;
import org.yuktisetu.drive.model.CodingReviewDecision;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationCodingReviewRepository extends JpaRepository<ApplicationCodingReview, Long> {

    List<ApplicationCodingReview> findByApplicationIdIn(Collection<Long> applicationIds);

    List<ApplicationCodingReview> findByPostingId(Long postingId);

    /** Decisions on applications still in the sealed pool, by kind -- the seat arithmetic. */
    @Query(value = """
            SELECT count(*) FROM application_coding_reviews r
              JOIN job_applications a ON a.id = r.application_id
             WHERE r.posting_id = :postingId
               AND r.decision = :decision
               AND a.status = 'IN_REVIEW'
               AND a.is_deleted = false
            """, nativeQuery = true)
    long countInPool(@Param("postingId") Long postingId, @Param("decision") String decision);

    default long countInPool(Long postingId, CodingReviewDecision decision) {
        return countInPool(postingId, decision.name());
    }
}
