package org.yuktisetu.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.profile.db.StudentProfile;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    // Primary lookup — every profile operation starts from the logged-in user's ID (from JWT).
    Optional<StudentProfile> findByUserIdAndUser_IsDeletedFalse(Long userId);

    boolean existsByUserIdAndUser_IsDeletedFalse(Long userId);

    // Cascade guard for hard delete — includes soft-deleted users on purpose,
    // since a soft-deleted account still owns a profile row (and, through it,
    // projects/achievements/coding profiles that cascade from it).
    boolean existsByUser_Id(Long userId);
}