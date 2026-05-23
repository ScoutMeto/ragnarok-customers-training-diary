package com.ragnarok.ragnarok_customers_training_diary.training;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingRepository extends JpaRepository<TrainingEntity, Long> {

    /**
     * Tréninky daného klienta, nejnovější nahoře.
     */
    List<TrainingEntity> findByOwner_IdOrderByTrainingDateDescIdDesc(Long ownerId);

    /**
     * Pro detail/edit: load training + verify ownership v jednom dotazu.
     */
    Optional<TrainingEntity> findByIdAndOwner_Id(Long id, Long ownerId);

    /**
     * Tréninky klienta v daném datovém rozsahu (pro statistiky a dashboard).
     */
    List<TrainingEntity> findByOwner_IdAndTrainingDateBetween(Long ownerId, LocalDate from, LocalDate to);

    long countByOwner_Id(Long ownerId);
}
