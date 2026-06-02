package com.ragnarok.ragnarok_customers_training_diary.training.types.interval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntervalConfigRepository extends JpaRepository<IntervalConfigEntity, Long> {
}
