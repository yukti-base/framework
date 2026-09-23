package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.PostingReapplicationPolicy;

import java.util.Optional;

@Repository
public interface PostingReapplicationPolicyRepository extends JpaRepository<PostingReapplicationPolicy, Long> {

    Optional<PostingReapplicationPolicy> findByPosting_IdAndIsDeletedFalse(Long postingId);

    boolean existsByPosting_IdAndIsDeletedFalse(Long postingId);
}
