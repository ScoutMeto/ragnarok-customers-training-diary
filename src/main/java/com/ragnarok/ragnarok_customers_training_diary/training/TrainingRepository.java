package com.ragnarok.ragnarok_customers_training_diary.training;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingRepository extends JpaRepository<TrainingEntity, Long> {

    /**
     * Phase 14/15 (B8): filtr vlastních tréninků podle tagu / názvu cviku / vlaječky.
     * Tag matchuje na úrovni tréninku NEBO některého jeho cviku. Všechny filtry
     * jsou volitelné (null/false = neaktivní).
     */
    @Query("""
            SELECT DISTINCT t FROM TrainingEntity t
              LEFT JOIN t.exercises e
              LEFT JOIN e.catalogItem ci
            WHERE t.owner.id = :ownerId
              AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE
              AND (:flaggedOnly = false OR t.flagged = true)
              AND (:exerciseName IS NULL OR COALESCE(ci.name, e.customName) = :exerciseName)
              AND (:tagId IS NULL
                   OR EXISTS (SELECT 1 FROM t.tags tt WHERE tt.id = :tagId)
                   OR EXISTS (SELECT 1 FROM t.exercises ex2 JOIN ex2.tags et WHERE et.id = :tagId))
            ORDER BY t.trainingDate DESC, t.id DESC
            """)
    List<TrainingEntity> findFiltered(@Param("ownerId") Long ownerId,
                                      @Param("tagId") Long tagId,
                                      @Param("exerciseName") String exerciseName,
                                      @Param("flaggedOnly") boolean flaggedOnly);

    // -----------------------------------------------------------------------------
    // PRIVATE — vlastní tréninky klienta
    // -----------------------------------------------------------------------------

    /**
     * Vlastní (PRIVATE) tréninky klienta, nejnovější nahoře.
     */
    List<TrainingEntity> findByOwner_IdAndVisibilityOrderByTrainingDateDescIdDesc(
            Long ownerId, TrainingVisibility visibility);

    /**
     * Pro detail/edit klientova tréninku: load + verify ownership v jednom dotazu.
     */
    Optional<TrainingEntity> findByIdAndOwner_Id(Long id, Long ownerId);

    /**
     * ScoutMeto kolo 5: poslední PRIVATE trénink klienta s vyplněnou hmotností —
     * pro předvyplnění bodyweight v novém tréninku.
     */
    Optional<TrainingEntity> findFirstByOwner_IdAndVisibilityAndBodyweightKgIsNotNullOrderByTrainingDateDescIdDesc(
            Long ownerId, TrainingVisibility visibility);

    /** Tréninky klienta v daném datovém rozsahu (pro statistiky a dashboard). */
    List<TrainingEntity> findByOwner_IdAndTrainingDateBetween(Long ownerId, LocalDate from, LocalDate to);

    long countByOwner_Id(Long ownerId);

    // -----------------------------------------------------------------------------
    // GROUP — skupinové tréninky vytvořené adminem, viditelné všem
    // -----------------------------------------------------------------------------

    /** Skupinové tréninky pro konkrétní den (klient view: dashboard / today section). */
    List<TrainingEntity> findByVisibilityAndTrainingDateOrderByStartTimeAsc(
            TrainingVisibility visibility, LocalDate date);

    /** ScoutMeto kolo 7: klientský den — jen publikované skupinové tréninky. */
    List<TrainingEntity> findByVisibilityAndTrainingDateAndPublishedTrueOrderByStartTimeAsc(
            TrainingVisibility visibility, LocalDate date);

    /** Všechny skupinové tréninky pro admin sekci, nejnovější nahoře. */
    List<TrainingEntity> findByVisibilityOrderByTrainingDateDescIdDesc(TrainingVisibility visibility);

    /** ScoutMeto kolo 7: stránkovaný admin výpis (řazení dodá Pageable — createdAt desc). */
    org.springframework.data.domain.Page<TrainingEntity> findByVisibility(
            TrainingVisibility visibility, org.springframework.data.domain.Pageable pageable);

    // -----------------------------------------------------------------------------
    // TEMPLATE — šablony v admin sekci (Phase 8)
    // -----------------------------------------------------------------------------

    /** Všechny šablony, nejnovější nahoře (podle created_at). */
    List<TrainingEntity> findByVisibilityOrderByCreatedAtDesc(TrainingVisibility visibility);
}
