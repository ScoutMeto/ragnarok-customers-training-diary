package com.ragnarok.ragnarok_customers_training_diary.entity.repository;

import com.ragnarok.ragnarok_customers_training_diary.entity.CircuitEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CircuitRepository extends JpaRepository<CircuitEntity, Long> {

    List<CircuitEntity> findByExerciseTrainingTrainingId(Long trainingId);
}