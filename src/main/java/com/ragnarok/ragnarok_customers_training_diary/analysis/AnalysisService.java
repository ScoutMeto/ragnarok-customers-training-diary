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

    /**
     * Phase 15 (B2-B4, B7): seznam unikátních názvů cviků, které klient někdy
     * zaznamenal (catalog name i custom name). Pro dropdown ve statistikách.
     */
    public List<String> listLoggedExerciseNames(AccountEntity owner) {
        @SuppressWarnings("unchecked")
        List<String> names = em.createQuery(
                "SELECT DISTINCT COALESCE(ci.name, e.customName) " +
                "FROM TrainingExerciseEntity e " +
                "  JOIN e.training t " +
                "  LEFT JOIN e.catalogItem ci " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND COALESCE(ci.name, e.customName) IS NOT NULL " +
                "ORDER BY COALESCE(ci.name, e.customName) ASC")
                .setParameter("ownerId", owner.getId())
                .getResultList();
        return names;
    }

    /**
     * Phase 15 (B2-B4, B7): souhrnné metriky pro vybraný cvik (podle názvu —
     * sjednocuje catalog i custom záznamy stejného jména) v daném období:
     *  - totalVolumeKg = SUM(weight * reps)
     *  - totalSets     = počet setů
     *  - totalReps     = SUM(reps)
     *  - maxReps       = nejvyšší počet opakování v jednom setu
     *  - maxWeightKg   = nejvyšší váha
     */
    public ExerciseStats exerciseStats(AccountEntity owner, String exerciseName,
                                        LocalDate from, LocalDate to) {
        Object[] r = (Object[]) em.createQuery(
                "SELECT COALESCE(SUM(s.weightKg * s.reps), 0), " +
                "       COUNT(s), " +
                "       COALESCE(SUM(s.reps), 0), " +
                "       MAX(s.reps), " +
                "       MAX(s.weightKg) " +
                "FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "  LEFT JOIN e.catalogItem ci " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND COALESCE(ci.name, e.customName) = :name " +
                "  AND t.trainingDate BETWEEN :from AND :to")
                .setParameter("ownerId", owner.getId())
                .setParameter("name", exerciseName)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return new ExerciseStats(
                exerciseName,
                toBigDecimal(r[0]),
                r[1] != null ? ((Number) r[1]).longValue() : 0L,
                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                r[3] != null ? ((Number) r[3]).intValue() : 0,
                r[4] != null ? toBigDecimal(r[4]) : null);
    }

    /**
     * Phase 15 (B1/B5): seznam per-exercise tagů, které klient použil u cviků
     * (pro multi-select ve statistikách).
     */
    public List<com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity> listUsedExerciseTags(AccountEntity owner) {
        @SuppressWarnings("unchecked")
        List<com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity> tags = em.createQuery(
                "SELECT DISTINCT tag FROM TrainingExerciseEntity e " +
                "  JOIN e.training t " +
                "  JOIN e.tags tag " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "ORDER BY tag.name ASC")
                .setParameter("ownerId", owner.getId())
                .getResultList();
        return tags;
    }

    /**
     * Phase 15 (B1/B5): objem/série/opakování pro cviky, které mají alespoň jeden
     * z vybraných per-exercise tagů (OR semantika). EXISTS místo JOIN — cvik
     * s víc matching tagy se počítá jen jednou (nedojde k duplikaci přes set).
     */
    public ExerciseStats statsByExerciseTags(AccountEntity owner, java.util.Collection<Long> tagIds,
                                              LocalDate from, LocalDate to) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ExerciseStats(null, java.math.BigDecimal.ZERO, 0L, 0L, 0, null);
        }
        Object[] r = (Object[]) em.createQuery(
                "SELECT COALESCE(SUM(s.weightKg * s.reps), 0), " +
                "       COUNT(s), " +
                "       COALESCE(SUM(s.reps), 0), " +
                "       MAX(s.reps), " +
                "       MAX(s.weightKg) " +
                "FROM ExerciseSetEntity s " +
                "  JOIN s.trainingExercise e " +
                "  JOIN e.training t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "  AND EXISTS (SELECT 1 FROM e.tags tg WHERE tg.id IN :tagIds)")
                .setParameter("ownerId", owner.getId())
                .setParameter("tagIds", tagIds)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return new ExerciseStats(
                null,
                toBigDecimal(r[0]),
                r[1] != null ? ((Number) r[1]).longValue() : 0L,
                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                r[3] != null ? ((Number) r[3]).intValue() : 0,
                r[4] != null ? toBigDecimal(r[4]) : null);
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

    /**
     * Volume podle úrovně obtížnosti — 3 sloupce (Lehký / Střední / Těžký).
     * Phase 9 (A13): difficulty má 9 zaměření, agregujeme je do 3 úrovní
     * přes {@link com.ragnarok.ragnarok_customers_training_diary.training.TrainingDifficulty.Level}.
     */
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

        // Agreguj 9 zaměření → 3 úrovně (zachová pořadí LIGHT, MEDIUM, HARD)
        var byLevel = new java.util.EnumMap<
                com.ragnarok.ragnarok_customers_training_diary.training.TrainingDifficulty.Level, BigDecimal>(
                com.ragnarok.ragnarok_customers_training_diary.training.TrainingDifficulty.Level.class);
        for (Object[] r : rows) {
            if (r[0] == null) continue;
            var focus = (com.ragnarok.ragnarok_customers_training_diary.training.TrainingDifficulty) r[0];
            byLevel.merge(focus.getLevel(), toBigDecimal(r[1]), BigDecimal::add);
        }
        List<LabelValuePoint> result = new ArrayList<>();
        for (var lvl : com.ragnarok.ragnarok_customers_training_diary.training.TrainingDifficulty.Level.values()) {
            BigDecimal v = byLevel.get(lvl);
            if (v != null && v.signum() != 0) {
                result.add(new LabelValuePoint(lvl.getLabel(), v));
            }
        }
        return result;
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

    /**
     * ScoutMeto kolo 5: data pro cyklus-kalendář (jen ženy). Pro každý den s tréninkem
     * vrací počet tréninků + den cyklu + fázi (z prvního tréninku dne, který má cyklus vyplněný).
     */
    public Map<LocalDate, CycleDayInfo> cycleCalendar(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.trainingDate, t.cycleDay, t.cyclePhase FROM TrainingEntity t " +
                "WHERE t.owner.id = :ownerId " +
                "  AND t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.trainingDate BETWEEN :from AND :to " +
                "ORDER BY t.trainingDate ASC, t.id ASC")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        Map<LocalDate, CycleDayInfo> map = new TreeMap<>();
        for (Object[] r : rows) {
            LocalDate date = (LocalDate) r[0];
            Short cycleDay = r[1] != null ? ((Number) r[1]).shortValue() : null;
            var phase = (com.ragnarok.ragnarok_customers_training_diary.training.CyclePhase) r[2];
            CycleDayInfo existing = map.get(date);
            if (existing == null) {
                map.put(date, new CycleDayInfo(1, cycleDay, phaseLetter(phase), phaseLabel(phase)));
            } else {
                // další trénink téhož dne: zvýšit počet, doplnit cyklus, pokud chyběl
                Short day = existing.cycleDay() != null ? existing.cycleDay() : cycleDay;
                String letter = existing.phaseLetter() != null ? existing.phaseLetter() : phaseLetter(phase);
                String label = existing.phaseLabel() != null ? existing.phaseLabel() : phaseLabel(phase);
                map.put(date, new CycleDayInfo(existing.count() + 1, day, letter, label));
            }
        }
        return map;
    }

    private static String phaseLetter(com.ragnarok.ragnarok_customers_training_diary.training.CyclePhase p) {
        return p != null ? p.getLetter() : null;
    }

    private static String phaseLabel(com.ragnarok.ragnarok_customers_training_diary.training.CyclePhase p) {
        return p != null ? p.getLabel() : null;
    }

    /** Záznam dne v cyklus-kalendáři. */
    public record CycleDayInfo(int count, Short cycleDay, String phaseLetter, String phaseLabel) {}

    // -----------------------------------------------------------------------------
    // Admin overview — napříč všemi klienty
    // -----------------------------------------------------------------------------

    public AdminOverview adminOverview(LocalDate from, LocalDate to) {
        Long activeAccounts = (Long) em.createQuery(
                "SELECT COUNT(a) FROM AccountEntity a WHERE a.deletedAt IS NULL " +
                "  AND a.role = com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.USER")
                .getSingleResult();

        // Phase 9 (D2): "Počet záznamů" = počet PRIVATE tréninků klientů (role USER, ne admini)
        // v daném období. Vylučujeme admin účty (admin si může vést vlastní deník, ale do
        // gym přehledu se nezapočítává).
        Long totalRecords = (Long) em.createQuery(
                "SELECT COUNT(t) FROM TrainingEntity t " +
                "WHERE t.visibility = com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility.PRIVATE " +
                "  AND t.owner IS NOT NULL " +
                "  AND t.owner.role = com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.USER " +
                "  AND t.trainingDate BETWEEN :from AND :to")
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
                "  AND t.owner.role = com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.USER " +
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
                totalRecords.intValue(),
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

    /** Phase 15: souhrnné metriky pro jeden cvik. */
    public record ExerciseStats(
            String exerciseName,
            BigDecimal totalVolumeKg,
            long totalSets,
            long totalReps,
            int maxReps,
            BigDecimal maxWeightKg
    ) {}
    public record TopClient(String firstName, String lastName, String email, int trainingCount) {}

    /**
     * Gym overview pro admina. Phase 9 (D1-D3): odstraněn gymTotalVolumeKg
     * a totalGroupTrainings, totalPrivateTrainings přejmenován na totalRecords
     * (počet záznamů do deníků klientů, mimo adminy).
     */
    public record AdminOverview(
            int activeClients,
            int totalRecords,
            List<TopClient> topActiveClients
    ) {}
}
