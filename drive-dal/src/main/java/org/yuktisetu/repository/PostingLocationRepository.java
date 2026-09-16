package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.PostingLocation;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostingLocationRepository extends JpaRepository<PostingLocation, Long> {

    List<PostingLocation> findByPosting_IdAndIsDeletedFalse(Long postingId);

    /**
     * Every row for a whole page of postings in ONE query. Called instead of the
     * single-posting lookup above when rendering a list -- otherwise a 50-row page
     * issues 50 of these, which is the N+1 the standing rule forbids.
     */
    List<PostingLocation> findByPosting_IdInAndIsDeletedFalse(Collection<Long> postingIds);

    void deleteByPosting_Id(Long postingId);
}
