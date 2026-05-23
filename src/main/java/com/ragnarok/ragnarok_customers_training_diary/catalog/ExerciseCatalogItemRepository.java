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
