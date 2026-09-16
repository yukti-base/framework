package org.yuktisetu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.db.PostingTarget;
import org.yuktisetu.db.StudentProfile;

import java.util.Collection;
import java.util.List;

/**
 * Reads across drive-dal, user-dal and institute-dal to answer "which students
 * does this posting target, and what are their academic facts".
 *
 * Hung off PostingTarget purely so Spring Data has a managed root; both queries
 * below are self-contained.
 */
@Repository
public interface EligibilityCandidateRepository extends JpaRepository<PostingTarget, Long> {

    /**
     * The targeting phase: every active, onboarded student whose college,
     * department, degree and batch match one of this posting's targets, paired
     * with the target that matched them.
     *
     * Native SQL on purpose. The rule "a target with department_id IS NULL means
     * every department in that college" has to survive exactly, and in JPQL
     * navigating a nullable association (t.department.id) can emit an implicit
     * INNER join that silently drops the very rows the IS NULL branch is supposed
     * to admit -- a bug whose symptom is a whole department missing from a drive
     * with nothing in the logs. Written out as SQL, the semantics are visible and
     * cannot drift with a Hibernate upgrade.
     *
     * Filters are the standing rule for role queries: active assignment, not
     * soft-deleted, account ACTIVE. A PENDING_ACTIVATION invitee or a SUSPENDED
     * student is not a candidate.
     *
     * A student matching two targets (say one restricted by degree and one not)
     * yields two rows; the caller keeps the first and ignores the rest.
     */
    @Query(value = """
            SELECT DISTINCT u.id AS student_user_id, t.id AS target_id
            FROM posting_targets t
            JOIN user_role_assignments ura
                 ON ura.college_id = t.college_id
                AND (t.department_id IS NULL OR ura.dept_id = t.department_id)
                AND (t.degree IS NULL OR ura.degree = t.degree)
            JOIN users u ON u.id = ura.user_id
            JOIN student_profiles sp ON sp.user_id = u.id
            WHERE t.posting_id = :postingId
              AND t.is_deleted = false
              AND ura.role = 'STUDENT'
              AND ura.is_active = true
              AND u.is_deleted = false
              AND u.status = 'ACTIVE'
              AND sp.graduation_year = t.batch_year
            ORDER BY u.id, t.id
            """, nativeQuery = true)
    List<Object[]> findTargetedStudents(@Param("postingId") Long postingId);

    /**
     * The academic facts for a batch of candidates, in one query rather than one
     * per student -- a 600-applicant drive must not become 600 round trips.
     */
    @Query("""
            select sp from StudentProfile sp
            where sp.user.id in :studentUserIds
              and sp.user.isDeleted = false
            """)
    List<StudentProfile> findProfilesByUserIds(@Param("studentUserIds") Collection<Long> studentUserIds);

    /**
     * Resolved custom-criterion values for a batch of students, as
     * (userId, fieldKey, valueString, valueNumber, valueBoolean, valueDate).
     *
     * Only dynamic fields that are still active count: a field the TnP disabled
     * must stop gating drives immediately, not keep filtering on last season's
     * data.
     */
    @Query(value = """
            SELECT sp.user_id        AS student_user_id,
                   d.field_key       AS field_key,
                   v.value_string    AS value_string,
                   v.value_number    AS value_number,
                   v.value_boolean   AS value_boolean,
                   v.value_date      AS value_date
            FROM student_dynamic_field_values v
            JOIN student_profiles sp ON sp.id = v.student_profile_id
            JOIN dynamic_field_definitions d ON d.id = v.field_definition_id
            WHERE sp.user_id IN (:studentUserIds)
              AND d.is_active = true
              AND d.field_key IN (:fieldKeys)
            """, nativeQuery = true)
    List<Object[]> findDynamicFieldValues(@Param("studentUserIds") Collection<Long> studentUserIds,
                                          @Param("fieldKeys") Collection<String> fieldKeys);
}
