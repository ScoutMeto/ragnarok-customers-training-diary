package com.ragnarok.ragnarok_customers_training_diary.entity.repository;

import com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomizingExerciseRepository extends JpaRepository<CustomizingExerciseEntity, Long> {

    List<CustomizingExerciseEntity> findByExerciseTrainingTrainingId(Long trainingId);
}