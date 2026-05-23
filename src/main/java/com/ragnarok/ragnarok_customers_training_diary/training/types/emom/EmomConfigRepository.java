package com.ragnarok.ragnarok_customers_training_diary.training.types.emom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmomConfigRepository extends JpaRepository<EmomConfigEntity, Long> {
}
