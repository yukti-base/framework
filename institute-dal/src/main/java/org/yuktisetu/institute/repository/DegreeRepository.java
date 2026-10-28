package org.yuktisetu.institute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.yuktisetu.institute.db.Degree;

import java.util.List;
import java.util.Optional;

@Repository
public interface DegreeRepository extends JpaRepository<Degree, Long> {

    Optional<Degree> findByIdAndIsDeletedFalse(Long id);

    List<Degree> findByIsDeletedFalseOrderByNameAsc();

    List<Degree> findByIdInAndIsDeletedFalse(java.util.Collection<Long> ids);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);
}
