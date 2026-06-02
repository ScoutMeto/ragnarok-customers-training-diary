package com.ragnarok.ragnarok_customers_training_diary.training.types.strongfirst;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StrongFirstLadderConfigRepository extends JpaRepository<StrongFirstLadderConfigEntity, Long> {
}
