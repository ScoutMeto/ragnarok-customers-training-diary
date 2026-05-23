package com.ragnarok.ragnarok_customers_training_diary.tag;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingTagRepository extends JpaRepository<TrainingTagEntity, Long> {

    /**
     * Tagy viditelné konkrétnímu uživateli — systémové + jeho vlastní.
     */
    @Query("""
            SELECT t FROM TrainingTagEntity t
            WHERE t.isSystem = true
               OR t.owner.id = :ownerId
            ORDER BY t.isSystem DESC, t.name ASC
            """)
    List<TrainingTagEntity> findVisibleTo(@Param("ownerId") Long ownerId);

    List<TrainingTagEntity> findByIsSystemTrueOrderByNameAsc();
}
