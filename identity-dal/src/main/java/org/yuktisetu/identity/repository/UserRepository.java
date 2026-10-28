package org.yuktisetu.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.identity.db.User;
import org.yuktisetu.identity.model.NotifyTarget;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Excludes soft-deleted rows by default — login/lookup paths must never
    // resolve a deleted account. Callers needing deleted rows (admin restore
    // screens) go through a separate explicit query, not this one.
    Optional<User> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndIsDeletedFalse(String email);

    // The "separate explicit query" the note above refers to: admin restore and
    // audit screens legitimately need the rows the default lookups hide.
    Optional<User> findByIdAndIsDeletedTrue(Long id);

    // Cascade guard for hard delete — users.deleted_by points at another user,
    // so whoever soft-deleted someone cannot themselves be removed while that
    // reference still stands.
    long countByDeletedBy(Long actorUserId);

    /**
     * One person's notification identity -- id, address, name -- and nothing else.
     *
     * Exists so a single-recipient dispatch does not have to load a whole managed
     * User just to read two strings off it. The apply and withdraw paths hold only
     * a getReferenceById proxy for the student, and touching a field on that proxy
     * would trigger a full entity load anyway; this makes the cost explicit and
     * bounded instead.
     */
    @Query("""
            select new org.yuktisetu.identity.model.NotifyTarget(
                       u.id, u.email, u.firstName, u.lastName)
              from User u
             where u.id = :userId
               and u.isDeleted = false
            """)
    Optional<NotifyTarget> findNotifyTarget(@Param("userId") Long userId);

    /** The same, for many people in one query -- a whole shortlist, never one per student. */
    @Query("""
            select new org.yuktisetu.identity.model.NotifyTarget(
                       u.id, u.email, u.firstName, u.lastName)
              from User u
             where u.id in :userIds
               and u.isDeleted = false
            """)
    java.util.List<NotifyTarget> findNotifyTargets(@Param("userIds") java.util.Collection<Long> userIds);
}
