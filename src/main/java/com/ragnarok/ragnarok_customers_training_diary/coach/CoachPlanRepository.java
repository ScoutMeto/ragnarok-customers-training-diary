package com.ragnarok.ragnarok_customers_training_diary.coach;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CoachPlanRepository extends JpaRepository<CoachPlanEntity, Long> {

    /** Všechny plány klienta (admin sekce — historie). */
    List<CoachPlanEntity> findByClient_IdOrderByValidFromDesc(Long clientId);

    /** Aktuálně platný plán pro daný den. */
    @Query("""
            SELECT p FROM CoachPlanEntity p
            WHERE p.client.id = :clientId
              AND p.validFrom <= :date
              AND (p.validTo IS NULL OR p.validTo >= :date)
            ORDER BY p.validFrom DESC
            """)
    Optional<CoachPlanEntity> findActiveForClient(@Param("clientId") Long clientId,
                                                   @Param("date") LocalDate date);
}
