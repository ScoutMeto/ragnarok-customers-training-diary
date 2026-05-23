package com.ragnarok.ragnarok_customers_training_diary.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSetRepository extends JpaRepository<ExerciseSetEntity, Long> {
}
