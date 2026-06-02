package com.ragnarok.ragnarok_customers_training_diary.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingExerciseRepository extends JpaRepository<TrainingExerciseEntity, Long> {

    /**
     * Phase 14 (A16): cviky označené korunkou, patřící do PRIVATE tréninků vlastníka.
     * Řazeno od nejnovějšího tréninku.
     */
    @Query("SELECT e FROM TrainingExerciseEntity e " +
           "  JOIN e.training t " +
           "WHERE e.starred = TRUE " +
           "  AND t.owner.id = :ownerId " +
           "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
           "ORDER BY t.trainingDate DESC, e.orderIndex ASC")
    List<TrainingExerciseEntity> findStarredForOwner(@Param("ownerId") Long ownerId);

    /** Phase 15/B8: distinct názvy cviků klienta (pro filtr dropdown v deníku). */
    @Query("SELECT DISTINCT COALESCE(ci.name, e.customName) " +
           "FROM TrainingExerciseEntity e " +
           "  JOIN e.training t " +
           "  LEFT JOIN e.catalogItem ci " +
           "WHERE t.owner.id = :ownerId " +
           "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
           "  AND COALESCE(ci.name, e.customName) IS NOT NULL " +
           "ORDER BY COALESCE(ci.name, e.customName) ASC")
    List<String> findDistinctExerciseNames(@Param("ownerId") Long ownerId);
}
