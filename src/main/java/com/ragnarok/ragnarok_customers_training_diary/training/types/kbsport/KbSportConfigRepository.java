package com.ragnarok.ragnarok_customers_training_diary.training.types.kbsport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KbSportConfigRepository extends JpaRepository<KbSportConfigEntity, Long> {
}
