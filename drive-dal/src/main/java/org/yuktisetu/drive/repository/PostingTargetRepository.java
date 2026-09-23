package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.PostingTarget;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostingTargetRepository extends JpaRepository<PostingTarget, Long> {

    List<PostingTarget> findByPosting_IdAndIsDeletedFalse(Long postingId);

    /**
     * Every row for a whole page of postings in ONE query. Called instead of the
     * single-posting lookup above when rendering a list -- otherwise a 50-row page
     * issues 50 of these, which is the N+1 the standing rule forbids.
     */
    List<PostingTarget> findByPosting_IdInAndIsDeletedFalse(Collection<Long> postingIds);

    boolean existsByPosting_IdAndTargetKeyAndIsDeletedFalse(Long postingId, String targetKey);

    /** Publish precondition: a posting with no targets has no audience. */
    long countByPosting_IdAndIsDeletedFalse(Long postingId);

    void deleteByPosting_Id(Long postingId);
}
