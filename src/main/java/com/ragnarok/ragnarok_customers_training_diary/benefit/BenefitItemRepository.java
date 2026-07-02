package com.ragnarok.ragnarok_customers_training_diary.benefit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenefitItemRepository extends JpaRepository<BenefitItemEntity, Long> {

    List<BenefitItemEntity> findAllByOrderByPositionAscIdAsc();
}
