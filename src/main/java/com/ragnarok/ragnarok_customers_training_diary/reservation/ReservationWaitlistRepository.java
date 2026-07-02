package com.ragnarok.ragnarok_customers_training_diary.reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationWaitlistRepository extends JpaRepository<ReservationWaitlistEntity, Long> {

    /** Náhradníci na lekci v pořadí přihlášení (FIFO). */
    List<ReservationWaitlistEntity> findByExtTrainingIdAndStatusOrderByCreatedAtAscIdAsc(
            Long extTrainingId, ReservationWaitlistEntity.Status status);

    long countByExtTrainingIdAndStatus(Long extTrainingId, ReservationWaitlistEntity.Status status);

    Optional<ReservationWaitlistEntity> findByAccount_IdAndExtTrainingId(Long accountId, Long extTrainingId);

    /** Všechny záznamy uživatele (pro UI mapu lekce → můj waitlist stav). */
    List<ReservationWaitlistEntity> findByAccount_Id(Long accountId);

    /** ID lekcí, na které někdo čeká a ještě nezačaly — pro kontrolní promo job. */
    @Query("SELECT DISTINCT w.extTrainingId FROM ReservationWaitlistEntity w "
            + "WHERE w.status = :status AND (w.trainingStart IS NULL OR w.trainingStart > :now)")
    List<Long> findTrainingIdsWithWaiting(
            @org.springframework.data.repository.query.Param("status") ReservationWaitlistEntity.Status status,
            @org.springframework.data.repository.query.Param("now") LocalDateTime now);
}
