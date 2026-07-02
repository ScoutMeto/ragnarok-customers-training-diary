package com.ragnarok.ragnarok_customers_training_diary.coach;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TextPlanRepository extends JpaRepository<TextPlanEntity, Long> {

    /** Individuální šablony (originály u trenéra) — bez skupinových nabídek. */
    List<TextPlanEntity> findByTemplateTrueAndGroupOfferFalseOrderByCreatedAtDesc();

    /** ScoutMeto kolo 7: stránkovaný výpis individuálních šablon (createdAt desc přes Pageable). */
    org.springframework.data.domain.Page<TextPlanEntity> findByTemplateTrueAndGroupOfferFalse(
            org.springframework.data.domain.Pageable pageable);

    /** Kopie konkrétního uživatele. */
    List<TextPlanEntity> findByOwner_IdAndTemplateFalseOrderByCreatedAtDesc(Long ownerId);

    /** ScoutMeto kolo 6: skupinové textové nabídky (pro všechny uživatele). */
    List<TextPlanEntity> findByGroupOfferTrueOrderByCreatedAtDesc();

    /** ScoutMeto kolo 7: stránkovaný admin výpis skupinových nabídek. */
    org.springframework.data.domain.Page<TextPlanEntity> findByGroupOfferTrue(
            org.springframework.data.domain.Pageable pageable);

    /**
     * ScoutMeto kolo 7: nabídky viditelné uživatelům — publikované v aktuálním týdnu
     * (od pondělí). Nabídka minulého týdne s pondělkem zmizí.
     */
    List<TextPlanEntity> findByGroupOfferTrueAndPublishedTrueAndPublishedAtGreaterThanEqualOrderByPublishedAtDescIdDesc(
            java.time.LocalDate mondayOfWeek);

    /** Už si uživatel danou skupinovou nabídku přidal? (kopie se sourceTemplate = nabídka) */
    boolean existsByOwner_IdAndSourceTemplate_Id(Long ownerId, Long sourceTemplateId);

    /** Množina ID nabídek/šablon, které už uživatel má jako kopii (jeden dotaz, bez N+1). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT tp.sourceTemplate.id FROM TextPlanEntity tp " +
            "WHERE tp.owner.id = :ownerId AND tp.sourceTemplate.id IS NOT NULL")
    java.util.Set<Long> findAddedSourceIdsByOwnerId(
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId);
}
