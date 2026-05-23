package com.ragnarok.ragnarok_customers_training_diary.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingCommentRepository extends JpaRepository<TrainingCommentEntity, Long> {

    /** Komentáře k tréninku, nejstarší nahoře (chronologicky). */
    List<TrainingCommentEntity> findByTraining_IdOrderByCreatedAtAsc(Long trainingId);
}
