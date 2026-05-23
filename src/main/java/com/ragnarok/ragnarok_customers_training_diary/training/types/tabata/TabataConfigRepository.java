package com.ragnarok.ragnarok_customers_training_diary.training.types.tabata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TabataConfigRepository extends JpaRepository<TabataConfigEntity, Long> {
}
