package com.ragnarok.ragnarok_customers_training_diary.analysis;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistiky tréninkového deníku. Klientovy data se počítají z jeho PRIVATE tréninků
 * (visibility=PRIVATE, owner=klient). Pokud klient chce zaznamenat výkon ze
 * skupinového tréninku, použije „Zkopírovat do mého deníku" → vznikne PRIVATE kopie.
 *
 * <p>Admin overview agreguje napříč všemi klienty.
 */
@Service
@Transactional(readOnly = true)
public class AnalysisService {

    @PersistenceContext
    private EntityManager em;

    // -----------------------------------------------------------------------------
    // Klientské statistiky
    // -----------------------------------------------------------------------------

    /** Total volume per den (SUM(set.weightKg * set.reps)). */
    public List<DateValuePoint> totalVolumeByDay(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.trainingDate, " +
                "       COALESCE(SUM(s.weightKg * s.reps), 0) " +
                "FROM ExerciseSetEntity s " +
                "   JOIN s.trainingExercise e " +
                "   JOIN e.training t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL " +
                "GROUP BY t.trainingDate " +
                "ORDER BY t.trainingDate ASC")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(r -> new DateValuePoint((LocalDate) r[0], toBigDecimal(r[1])))
                .toList();
    }

    /** PR (max weight) per cvik z katalogu v daném období. */
    public BigDecimal maxWeightForExercise(AccountEntity owner, Long catalogItemId, LocalDate from, LocalDate to) {
        Object result = em.createQuery(
                "SELECT MAX(s.weightKg) FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND e.catalogItem.id = :catalogId " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND s.weightKg IS NOT NULL")
                .setParameter("ownerId", owner.getId())
                .setParameter("catalogId", catalogItemId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return result == null ? null : toBigDecimal(result);
    }

    /** Vývoj max-weight per cvik v čase (PR history). */
    public List<DateValuePoint> prHistory(AccountEntity owner, Long catalogItemId, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.trainingDate, MAX(s.weightKg) " +
                "FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND e.catalogItem.id = :catalogId " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND s.weightKg IS NOT NULL " +
                "GROUP BY t.trainingDate " +
                "ORDER BY t.trainingDate ASC")
                .setParameter("ownerId", owner.getId())
                .setParameter("catalogId", catalogItemId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(r -> new DateValuePoint((LocalDate) r[0], toBigDecimal(r[1])))
                .toList();
    }

    /** Počet setů per body region. */
    public List<LabelValuePoint> setsPerBodyRegion(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT e.catalogItem.bodyRegion, COUNT(s) " +
                "FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND e.catalogItem IS NOT NULL " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "GROUP BY e.catalogItem.bodyRegion")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(r -> new LabelValuePoint(r[0] != null ? r[0].toString() : "—", toBigDecimal(r[1])))
                .toList();
    }

    /** Počet setů per movement pattern. */
    public List<LabelValuePoint> setsPerMovementPattern(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT e.catalogItem.movementPattern, COUNT(s) " +
                "FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND e.catalogItem IS NOT NULL " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "GROUP BY e.catalogItem.movementPattern")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(r -> new LabelValuePoint(r[0] != null ? r[0].toString() : "—", toBigDecimal(r[1])))
                .toList();
    }

    /** Průměrné RPE tréninku per den (časový vývoj). */
    public List<DateValuePoint> rpeTrend(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.trainingDate, AVG(t.rpe) FROM TrainingEntity t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND t.rpe IS NOT NULL " +
                "GROUP BY t.trainingDate " +
                "ORDER BY t.trainingDate ASC")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(r -> new DateValuePoint((LocalDate) r[0], toBigDecimal(r[1])))
                .toList();
    }

    /** Heatmap: počet tréninků per den (pro GitHub-style heatmap). */
    public Map<LocalDate, Integer> frequencyHeatmap(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.trainingDate, COUNT(t) FROM TrainingEntity t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "GROUP BY t.trainingDate")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        Map<LocalDate, Integer> map = new TreeMap<>();
        for (Object[] r : rows) {
            map.put((LocalDate) r[0], ((Number) r[1]).intValue());
        }
        return map;
    }

    /** Volume per difficulty (LIGHT/MEDIUM/HARD). */
    public List<LabelValuePoint> volumePerDifficulty(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.difficulty, COALESCE(SUM(s.weightKg * s.reps), 0) " +
                "FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND t.difficulty IS NOT NULL " +
                "  AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL " +
                "GROUP BY t.difficulty")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(r -> new LabelValuePoint(r[0] != null ? r[0].toString() : "—", toBigDecimal(r[1])))
                .toList();
    }

    /** RPE per den — pro kalendářní heatmap (může být null kde tréning nebyl). */
    public Map<LocalDate, Short> rpePerDay(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.trainingDate, t.rpe FROM TrainingEntity t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND t.rpe IS NOT NULL " +
                "ORDER BY t.trainingDate ASC")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        Map<LocalDate, Short> map = new TreeMap<>();
        for (Object[] r : rows) {
            map.put((LocalDate) r[0], ((Number) r[1]).shortValue());
        }
        return map;
    }

    // -----------------------------------------------------------------------------
    // Admin overview — napříč všemi klienty
    // -----------------------------------------------------------------------------

    public AdminOverview adminOverview(LocalDate from, LocalDate to) {
        Long activeAccounts = (Long) em.createQuery(
                "SELECT COUNT(a) FROM AccountEntity a WHERE a.deletedAt IS NULL " +
                "  AND a.role = com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.USER")
                .getSingleResult();

        Long totalPrivateTrainings = (Long) em.createQuery(
                "SELECT COUNT(t) FROM TrainingEntity t " +
                "WHERE t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        Long totalGroupTrainings = (Long) em.createQuery(
                "SELECT COUNT(t) FROM TrainingEntity t " +
                "WHERE t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.GROUP " +
                "  AND t.trainingDate BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        BigDecimal gymTotalVolume = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM(s.weightKg * s.reps), 0) FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "WHERE t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        @SuppressWarnings("unchecked")
        List<Object[]> topActive = em.createQuery(
                "SELECT t.owner.firstName, t.owner.lastName, t.owner.email, COUNT(t) " +
                "FROM TrainingEntity t " +
                "WHERE t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND t.owner IS NOT NULL " +
                "GROUP BY t.owner.id, t.owner.firstName, t.owner.lastName, t.owner.email " +
                "ORDER BY COUNT(t) DESC")
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(10)
                .getResultList();
        List<TopClient> top = new ArrayList<>();
        for (Object[] r : topActive) {
            top.add(new TopClient(
                    (String) r[0], (String) r[1], (String) r[2],
                    ((Number) r[3]).intValue()));
        }

        return new AdminOverview(
                activeAccounts.intValue(),
                totalPrivateTrainings.intValue(),
                totalGroupTrainings.intValue(),
                gymTotalVolume,
                top);
    }

    // -----------------------------------------------------------------------------
    // Helpers + DTO records
    // -----------------------------------------------------------------------------

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }

    public record DateValuePoint(LocalDate date, BigDecimal value) {}
    public record LabelValuePoint(String label, BigDecimal value) {}
    public record TopClient(String firstName, String lastName, String email, int trainingCount) {}
    public record AdminOverview(
            int activeClients,
            int totalPrivateTrainings,
            int totalGroupTrainings,
            BigDecimal gymTotalVolumeKg,
            List<TopClient> topActiveClients
    ) {}
}
