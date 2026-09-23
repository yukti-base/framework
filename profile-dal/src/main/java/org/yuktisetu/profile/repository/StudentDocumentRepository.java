package org.yuktisetu.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yuktisetu.profile.db.StudentDocument;
import org.yuktisetu.profile.model.StudentDocumentType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findByStudent_IdAndIsDeletedFalseOrderByUploadedAtDesc(Long studentUserId);

    Optional<StudentDocument> findByIdAndIsDeletedFalse(Long id);

    /** The resume an application attaches. */
    Optional<StudentDocument> findByStudent_IdAndDocTypeAndIsPrimaryTrueAndIsDeletedFalse(
            Long studentUserId, StudentDocumentType docType);

    /**
     * Primary documents for a whole applicant list, in ONE query.
     *
     * The applicant pool and the Excel export both need every student's resume
     * at once; fetching them per row would be the N+1 this codebase's rules
     * forbid. Callers index the result by student id.
     */
    @Query("""
            select d from StudentDocument d
             where d.student.id in :studentUserIds
               and d.docType = :docType
               and d.isPrimary = true
               and d.isDeleted = false
            """)
    List<StudentDocument> findPrimaryForStudents(@Param("studentUserIds") Collection<Long> studentUserIds,
                                                 @Param("docType") StudentDocumentType docType);

    /** Clears the previous primary before a new one is promoted, in one statement. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentDocument d set d.isPrimary = false
             where d.student.id = :studentUserId
               and d.docType = :docType
               and d.isPrimary = true
            """)
    int clearPrimary(@Param("studentUserId") Long studentUserId,
                     @Param("docType") StudentDocumentType docType);
}
