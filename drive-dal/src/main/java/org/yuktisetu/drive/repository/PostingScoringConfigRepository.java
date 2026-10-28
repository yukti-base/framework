package org.yuktisetu.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.drive.db.PostingScoringConfig;

import java.util.Optional;

@Repository
public interface PostingScoringConfigRepository extends JpaRepository<PostingScoringConfig, Long> {

    Optional<PostingScoringConfig> findByPostingId(Long postingId);

    boolean existsByPostingId(Long postingId);
}
