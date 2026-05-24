package com.ragnarok.ragnarok_customers_training_diary.training;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingRepository extends JpaRepository<TrainingEntity, Long> {

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

    /** Tréninky klienta v daném datovém rozsahu (pro statistiky a dashboard). */
    List<TrainingEntity> findByOwner_IdAndTrainingDateBetween(Long ownerId, LocalDate from, LocalDate to);

    long countByOwner_Id(Long ownerId);

    // -----------------------------------------------------------------------------
    // GROUP — skupinové tréninky vytvořené adminem, viditelné všem
    // -----------------------------------------------------------------------------

    /** Skupinové tréninky pro konkrétní den (klient view: dashboard / today section). */
    List<TrainingEntity> findByVisibilityAndTrainingDateOrderByStartTimeAsc(
            TrainingVisibility visibility, LocalDate date);

    /** Všechny skupinové tréninky pro admin sekci, nejnovější nahoře. */
    List<TrainingEntity> findByVisibilityOrderByTrainingDateDescIdDesc(TrainingVisibility visibility);

    // -----------------------------------------------------------------------------
    // TEMPLATE — šablony v admin sekci (Phase 8)
    // -----------------------------------------------------------------------------

    /** Všechny šablony, nejnovější nahoře (podle created_at). */
    List<TrainingEntity> findByVisibilityOrderByCreatedAtDesc(TrainingVisibility visibility);
}
