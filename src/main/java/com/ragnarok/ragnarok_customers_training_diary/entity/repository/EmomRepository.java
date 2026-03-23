package com.ragnarok.ragnarok_customers_training_diary.entity.repository;

import com.ragnarok.ragnarok_customers_training_diary.entity.EmomEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmomRepository extends JpaRepository<EmomEntity, Long> {

    List<EmomEntity> findByExerciseTrainingTrainingId(Long trainingId);
}