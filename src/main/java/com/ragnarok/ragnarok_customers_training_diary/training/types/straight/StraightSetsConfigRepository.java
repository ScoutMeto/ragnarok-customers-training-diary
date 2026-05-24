package com.ragnarok.ragnarok_customers_training_diary.training.types.straight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StraightSetsConfigRepository extends JpaRepository<StraightSetsConfigEntity, Long> {
}
