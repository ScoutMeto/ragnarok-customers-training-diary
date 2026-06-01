package com.ragnarok.ragnarok_customers_training_diary.catalog;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseCatalogItemRepository extends JpaRepository<ExerciseCatalogItemEntity, Long> {

    List<ExerciseCatalogItemEntity> findByActiveTrueOrderByNameAsc();

    /**
     * Phase 17: cviky viditelné pro uživatele = systémové (is_system=true)
     * NEBO vlastní custom (created_by = uživatel). Jen aktivní.
     */
    @Query("""
            SELECT e FROM ExerciseCatalogItemEntity e
            WHERE e.active = true
              AND (e.isSystem = true OR e.createdBy.id = :userId)
            ORDER BY e.name ASC
            """)
    List<ExerciseCatalogItemEntity> findVisibleTo(@Param("userId") Long userId);

    /**
     * Search by (case-insensitive) substring v názvu. Vrací jen aktivní položky.
     */
    @Query("""
            SELECT e FROM ExerciseCatalogItemEntity e
            WHERE e.active = true
              AND LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY e.name ASC
            """)
    List<ExerciseCatalogItemEntity> searchByName(@Param("query") String query, Pageable pageable);
}
