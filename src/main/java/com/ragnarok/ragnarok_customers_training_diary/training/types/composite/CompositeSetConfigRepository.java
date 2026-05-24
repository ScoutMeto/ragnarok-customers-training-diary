package com.ragnarok.ragnarok_customers_training_diary.training.types.composite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompositeSetConfigRepository extends JpaRepository<CompositeSetConfigEntity, Long> {
}
