package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.PostingSkill;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostingSkillRepository extends JpaRepository<PostingSkill, Long> {

    List<PostingSkill> findByPosting_IdAndIsDeletedFalse(Long postingId);

    /**
     * Every row for a whole page of postings in ONE query. Called instead of the
     * single-posting lookup above when rendering a list -- otherwise a 50-row page
     * issues 50 of these, which is the N+1 the standing rule forbids.
     */
    List<PostingSkill> findByPosting_IdInAndIsDeletedFalse(Collection<Long> postingIds);

    List<PostingSkill> findByPosting_IdAndIsMandatoryTrueAndIsDeletedFalse(Long postingId);

    void deleteByPosting_Id(Long postingId);
}
