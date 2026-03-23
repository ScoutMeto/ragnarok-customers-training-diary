package com.ragnarok.ragnarok_customers_training_diary.entity.repository;

import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseRepository extends JpaRepository<ExerciseEntity, Long> {
}