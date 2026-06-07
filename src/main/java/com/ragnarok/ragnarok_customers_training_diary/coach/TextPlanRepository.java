package com.ragnarok.ragnarok_customers_training_diary.coach;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TextPlanRepository extends JpaRepository<TextPlanEntity, Long> {

    /** Šablony (originály u trenéra). */
    List<TextPlanEntity> findByTemplateTrueOrderByCreatedAtDesc();

    /** Kopie konkrétního uživatele. */
    List<TextPlanEntity> findByOwner_IdAndTemplateFalseOrderByCreatedAtDesc(Long ownerId);
}
