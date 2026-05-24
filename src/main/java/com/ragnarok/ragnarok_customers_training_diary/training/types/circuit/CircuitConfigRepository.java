package com.ragnarok.ragnarok_customers_training_diary.training.types.circuit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CircuitConfigRepository extends JpaRepository<CircuitConfigEntity, Long> {
}
