package com.ragnarok.ragnarok_customers_training_diary.analysis;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogLabels;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.SystemTag;
import com.ragnarok.ragnarok_customers_training_diary.tag.TagCategory;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistiky tréninkového deníku.
 *
 * <p>kolo 10: do statistik jde KAŽDÝ trénink, který má klient ve svém deníku — vlastní
 * i převzatý z nabídky, upravený i ponechaný beze změny. Filtruje se tedy jen podle
 * vlastníka ({@code t.owner}); skupinové a šablonové tréninky vlastníka nemají, takže
 * se do klientských statistik nedostanou samy o sobě, ale jejich kopie v deníku ano.
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
                r[4] != null ? toBigDecimal(r[4]) : null,
                0L,
                0L,
                0L);
    }

    /**
     * Phase 15 (B1/B5): seznam per-exercise tagů, které klient použil u cviků
     * (pro multi-select ve statistikách).
     */
    public List<com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity> listUsedExerciseTags(AccountEntity owner) {
        Map<Long, TrainingTagEntity> tagsById = new LinkedHashMap<>();
        addUsedExerciseTags(tagsById,
                "SELECT DISTINCT tag FROM TrainingExerciseEntity e " +
                "  JOIN e.training t " +
                "  JOIN e.tags tag " +
                "WHERE t.owner.id = :ownerId",
                owner.getId());
        addUsedExerciseTags(tagsById,
                "SELECT DISTINCT tag FROM CircuitStepEntity step " +
                "  JOIN step.circuitConfig cfg " +
                "  JOIN cfg.trainingExercise e " +
                "  JOIN e.training t " +
                "  JOIN step.tags tag " +
                "WHERE t.owner.id = :ownerId",
                owner.getId());
        addUsedExerciseTags(tagsById,
                "SELECT DISTINCT tag FROM AmrapStepEntity step " +
                "  JOIN step.amrapConfig cfg " +
                "  JOIN cfg.trainingExercise e " +
                "  JOIN e.training t " +
                "  JOIN step.tags tag " +
                "WHERE t.owner.id = :ownerId",
                owner.getId());

        return tagsById.values().stream()
                .sorted((a, b) -> {
                    String an = a.getName() != null ? a.getName() : "";
                    String bn = b.getName() != null ? b.getName() : "";
                    return an.compareToIgnoreCase(bn);
                })
                .toList();
    }

    /**
     * Phase 15 (B1/B5): objem/série/opakování pro cviky, které mají alespoň jeden
     * z vybraných per-exercise tagů (OR semantika). EXISTS místo JOIN — cvik
     * s víc matching tagy se počítá jen jednou (nedojde k duplikaci přes set).
     */
    public ExerciseStats statsByExerciseTags(AccountEntity owner, java.util.Collection<Long> tagIds,
                                              LocalDate from, LocalDate to) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ExerciseStats(null, java.math.BigDecimal.ZERO, 0L, 0L, 0, null, 0L, 0L, 0L);
        }
        ExerciseStatsAccumulator acc = new ExerciseStatsAccumulator();
        for (TrainingEntity t : loadTrainingsForAnalysis(owner, from, to)) {
            for (TrainingExerciseEntity ex : t.getExercises()) {
                collectStatsByTags(ex, tagIds, acc);
            }
        }
        return new ExerciseStats(
                null,
                acc.totalVolumeKg,
                acc.totalSets,
                acc.totalReps,
                acc.maxReps != null ? acc.maxReps : 0,
                acc.maxWeightKg,
                acc.totalMeters,
                acc.totalSeconds,
                acc.cardioSeconds);
    }

    /**
     * Počet setů pro oblast těla. Katalogové oblasti jsou výchozí zdroj, tagy
     * zadané při zápisu tréninku je doplňují. Počítáme i vnitřní kroky CIRCUIT/AMRAP.
     */
    public List<LabelValuePoint> setsPerBodyRegion(AccountEntity owner, LocalDate from, LocalDate to) {
        Map<String, Long> counts = orderedBodyRegionMap();
        Map<String, ExerciseCatalogItemEntity> catalogByName = catalogByName(owner);
        for (TrainingEntity t : loadTrainingsForAnalysis(owner, from, to)) {
            for (TrainingExerciseEntity ex : t.getExercises()) {
                for (AnalysisUnit unit : analysisUnits(ex, catalogByName)) {
                    for (String label : bodyRegionLabels(unit)) {
                        counts.merge(label, unit.count(), Long::sum);
                    }
                }
            }
        }
        return nonZeroPoints(counts);
    }

    /** Počet setů pro pohybové vzorce. Bere katalog i uživatelské tagy u cviku/kroku. */
    public List<LabelValuePoint> setsPerMovementPattern(AccountEntity owner, LocalDate from, LocalDate to) {
        Map<String, Long> counts = orderedMovementPatternMap();
        Map<String, ExerciseCatalogItemEntity> catalogByName = catalogByName(owner);
        for (TrainingEntity t : loadTrainingsForAnalysis(owner, from, to)) {
            for (TrainingExerciseEntity ex : t.getExercises()) {
                for (AnalysisUnit unit : analysisUnits(ex, catalogByName)) {
                    for (String label : movementPatternLabels(unit)) {
                        counts.merge(label, unit.count(), Long::sum);
                    }
                }
            }
        }
        return nonZeroPoints(counts);
    }
    /** Průměrné RPE tréninku per den (časový vývoj). */
    public List<DateValuePoint> rpeTrend(AccountEntity owner, LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT t.trainingDate, AVG(t.rpe) FROM TrainingEntity t " +
                "WHERE t.owner.id = :ownerId " +
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

    private List<TrainingEntity> loadTrainingsForAnalysis(AccountEntity owner, LocalDate from, LocalDate to) {
        return em.createQuery(
                "SELECT DISTINCT t FROM TrainingEntity t "
                        + "LEFT JOIN FETCH t.exercises "
                        + "WHERE t.owner.id = :ownerId "
                        + "  AND t.trainingDate BETWEEN :from AND :to "
                        + "ORDER BY t.trainingDate ASC",
                TrainingEntity.class)
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    private void addUsedExerciseTags(Map<Long, TrainingTagEntity> target, String jpql, Long ownerId) {
        List<TrainingTagEntity> tags = em.createQuery(jpql, TrainingTagEntity.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
        for (TrainingTagEntity tag : tags) {
            if (tag.getId() != null) {
                target.putIfAbsent(tag.getId(), tag);
            }
        }
    }

    private void collectStatsByTags(TrainingExerciseEntity ex, java.util.Collection<Long> tagIds,
                                    ExerciseStatsAccumulator acc) {
        TrainingExerciseType type = ex.getType() == null ? TrainingExerciseType.FREEFORM : ex.getType();
        if (type == TrainingExerciseType.CIRCUIT && ex.getCircuitConfig() != null) {
            var cfg = ex.getCircuitConfig();
            for (var step : cfg.getSteps()) {
                if (hasAnyTag(mergeTags(ex.getTags(), step.getTags()), tagIds)) {
                    collectCircuitStepStats(ex, cfg, step, acc);
                }
            }
            return;
        }
        if (type == TrainingExerciseType.AMRAP && ex.getAmrapConfig() != null) {
            var cfg = ex.getAmrapConfig();
            for (var step : cfg.getSteps()) {
                if (hasAnyTag(mergeTags(ex.getTags(), step.getTags()), tagIds)) {
                    collectAmrapStepStats(ex, cfg, step, acc);
                }
            }
            return;
        }
        if (hasAnyTag(ex.getTags(), tagIds)) {
            collectExerciseStats(ex, acc);
        }
    }

    private void collectExerciseStats(TrainingExerciseEntity ex, ExerciseStatsAccumulator acc) {
        BigDecimal eqWeight = equipmentWeight(ex);
        String exUnit = ex.getSetUnit();

        switch (ex.getType() == null ? TrainingExerciseType.FREEFORM : ex.getType()) {
            case FREEFORM -> ex.getSets().forEach(s ->
                    acc.addEffort(s.getReps(), exUnit,
                            s.getWeightKg() != null ? s.getWeightKg() : eqWeight, true));

            case EMOM -> {
                var cfg = ex.getEmomConfig();
                if (cfg == null) break;
                if (!cfg.getMinuteOverrides().isEmpty()) {
                    cfg.getMinuteOverrides().forEach(m ->
                            acc.addEffort(m.getReps(), exUnit,
                                    m.getWeightKg() != null ? m.getWeightKg() : eqWeight, true));
                } else {
                    long minutes = positive(cfg.getTotalMinutes());
                    BigDecimal weight = cfg.getDefaultWeightKg() != null ? cfg.getDefaultWeightKg() : eqWeight;
                    for (long i = 0; i < minutes; i++) {
                        acc.addEffort(cfg.getDefaultReps(), exUnit, weight, true);
                    }
                }
            }

            case TABATA -> {
                var cfg = ex.getTabataConfig();
                if (cfg == null) break;
                if (!cfg.getRoundOverrides().isEmpty()) {
                    cfg.getRoundOverrides().forEach(r ->
                            acc.addEffort(r.getReps(), exUnit,
                                    r.getWeightKg() != null ? r.getWeightKg() : eqWeight, true));
                } else {
                    long rounds = positive(cfg.getRounds());
                    BigDecimal weight = cfg.getDefaultWeightKg() != null ? cfg.getDefaultWeightKg() : eqWeight;
                    for (long i = 0; i < rounds; i++) {
                        acc.addEffort(cfg.getDefaultReps(), exUnit, weight, true);
                    }
                }
            }

            case CIRCUIT -> {
                var cfg = ex.getCircuitConfig();
                if (cfg != null) {
                    cfg.getSteps().forEach(step -> collectCircuitStepStats(ex, cfg, step, acc));
                }
            }

            case AMRAP -> {
                var cfg = ex.getAmrapConfig();
                if (cfg != null) {
                    cfg.getSteps().forEach(step -> collectAmrapStepStats(ex, cfg, step, acc));
                }
            }

            case LADDER, STEPLADDER, PYRAMID -> {
                var cfg = ex.getNumericSeriesConfig();
                if (cfg == null) break;
                cfg.getRows().forEach(row ->
                        acc.addEffort(row.getReps(), exUnit,
                                row.getWeightKg() != null ? row.getWeightKg()
                                        : (cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight), true));
            }

            case STRAIGHT_SETS -> {
                var cfg = ex.getStraightSetsConfig();
                if (cfg == null) break;
                if (!cfg.getRows().isEmpty()) {
                    cfg.getRows().forEach(row ->
                            acc.addEffort(row.getReps(), exUnit,
                                    row.getWeightKg() != null ? row.getWeightKg()
                                            : (cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight), true));
                } else {
                    BigDecimal weight = cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight;
                    for (long i = 0; i < positive(cfg.getSetCount()); i++) {
                        acc.addEffort(cfg.getRepsPerSet(), exUnit, weight, true);
                    }
                }
            }

            case INTERVAL -> {
                var cfg = ex.getIntervalConfig();
                if (cfg == null) break;
                BigDecimal weight = cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight;
                for (long i = 0; i < positive(cfg.getRounds()); i++) {
                    if (cfg.getWorkReps() != null && cfg.getWorkReps() > 0) {
                        acc.addEffort(cfg.getWorkReps(), exUnit, weight, true);
                    } else {
                        acc.addEffort(cfg.getWorkSeconds(), "SECONDS", weight, true);
                    }
                }
            }

            case STRONGFIRST_LADDER -> {
                var cfg = ex.getStrongFirstLadderConfig();
                if (cfg == null) break;
                cfg.getRows().forEach(row ->
                        acc.addEffort(row.getValue(), cfg.getRepUnit(),
                                row.getWeightKg() != null ? row.getWeightKg()
                                        : (cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight), true));
            }

            case KB_SPORT_TIME -> {
                var cfg = ex.getKbSportConfig();
                if (cfg == null) break;
                BigDecimal weight = cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight;
                acc.addEffort(cfg.getTotalReps(), "REPS", weight, true);
                acc.addSeconds(cfg.getTotalSeconds());
            }

            case CARDIO -> {
                var cfg = ex.getCardioConfig();
                if (cfg == null) break;
                acc.addCardioBlock(cfg.resolvedActiveSeconds(), cfg.getDistanceM(), cfg.getRepetitions());
            }
        }
    }

    private void collectCircuitStepStats(
            TrainingExerciseEntity ex,
            com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitConfigEntity cfg,
            com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitStepEntity step,
            ExerciseStatsAccumulator acc) {
        BigDecimal eqWeight = equipmentWeight(ex);
        if (!cfg.getRoundEntries().isEmpty()) {
            cfg.getRoundEntries().stream()
                    .filter(e -> !e.isSkipped())
                    .filter(e -> step.getOrderIndex() != null && step.getOrderIndex().equals(e.getStepOrder()))
                    .forEach(e -> acc.addEffort(e.getActualReps(), step.getRepUnit(),
                            e.getActualWeightKg() != null ? e.getActualWeightKg() : stepWeight(step, eqWeight),
                            true));
            return;
        }
        long rounds = positive(cfg.getRounds());
        for (long i = 0; i < rounds; i++) {
            acc.addEffort(step.getReps(), step.getRepUnit(), stepWeight(step, eqWeight), true);
        }
    }

    private void collectAmrapStepStats(
            TrainingExerciseEntity ex,
            com.ragnarok.ragnarok_customers_training_diary.training.types.amrap.AmrapConfigEntity cfg,
            com.ragnarok.ragnarok_customers_training_diary.training.types.amrap.AmrapStepEntity step,
            ExerciseStatsAccumulator acc) {
        BigDecimal eqWeight = equipmentWeight(ex);
        if (!cfg.getRoundEntries().isEmpty()) {
            cfg.getRoundEntries().stream()
                    .filter(e -> !e.isSkipped())
                    .filter(e -> step.getOrderIndex() != null && step.getOrderIndex().equals(e.getStepOrder()))
                    .forEach(e -> acc.addEffort(e.getActualReps(), step.getRepUnit(),
                            e.getActualWeightKg() != null ? e.getActualWeightKg() : amrapStepWeight(step, eqWeight),
                            true));
            return;
        }
        for (long i = 0; i < positive(cfg.getRoundsCompleted()); i++) {
            acc.addEffort(step.getReps(), step.getRepUnit(), amrapStepWeight(step, eqWeight), true);
        }
    }
    private Map<String, ExerciseCatalogItemEntity> catalogByName(AccountEntity owner) {
        List<ExerciseCatalogItemEntity> items = em.createQuery(
                "SELECT e FROM ExerciseCatalogItemEntity e "
                        + "WHERE e.active = true "
                        + "  AND (e.isSystem = true OR e.createdBy.id = :ownerId) "
                        + "ORDER BY e.isSystem ASC, e.name ASC",
                ExerciseCatalogItemEntity.class)
                .setParameter("ownerId", owner.getId())
                .getResultList();
        Map<String, ExerciseCatalogItemEntity> byName = new LinkedHashMap<>();
        for (ExerciseCatalogItemEntity item : items) {
            if (item.getName() != null) {
                byName.putIfAbsent(item.getName(), item);
            }
        }
        return byName;
    }

    private List<AnalysisUnit> analysisUnits(
            TrainingExerciseEntity ex,
            Map<String, ExerciseCatalogItemEntity> catalogByName) {
        TrainingExerciseType type = ex.getType() == null ? TrainingExerciseType.FREEFORM : ex.getType();
        if (type == TrainingExerciseType.CIRCUIT && ex.getCircuitConfig() != null) {
            List<AnalysisUnit> units = new ArrayList<>();
            var cfg = ex.getCircuitConfig();
            for (var step : cfg.getSteps()) {
                long count = circuitStepCount(cfg, step.getOrderIndex());
                if (count <= 0) continue;
                ExerciseCatalogItemEntity catalog = catalogByName.get(step.getName());
                units.add(new AnalysisUnit(count, mergeTags(ex.getTags(), step.getTags()),
                        bodyRegionsOf(catalog), movementPatternsOf(catalog)));
            }
            return units;
        }
        if (type == TrainingExerciseType.AMRAP && ex.getAmrapConfig() != null) {
            List<AnalysisUnit> units = new ArrayList<>();
            var cfg = ex.getAmrapConfig();
            for (var step : cfg.getSteps()) {
                long count = amrapStepCount(cfg, step.getOrderIndex());
                if (count <= 0) continue;
                ExerciseCatalogItemEntity catalog = catalogByName.get(step.getName());
                units.add(new AnalysisUnit(count, mergeTags(ex.getTags(), step.getTags()),
                        bodyRegionsOf(catalog), movementPatternsOf(catalog)));
            }
            return units;
        }

        long count = exerciseSetCount(ex);
        if (count <= 0) return List.of();
        return List.of(new AnalysisUnit(count, new LinkedHashSet<>(ex.getTags()),
                bodyRegionsOf(ex.getCatalogItem()), movementPatternsOf(ex.getCatalogItem())));
    }

    private static Set<String> bodyRegionsOf(ExerciseCatalogItemEntity item) {
        return item == null ? Set.of() : new LinkedHashSet<>(item.getBodyRegions());
    }

    private static Set<String> movementPatternsOf(ExerciseCatalogItemEntity item) {
        return item == null ? Set.of() : new LinkedHashSet<>(item.getMovementPatterns());
    }

    private static Set<TrainingTagEntity> mergeTags(Set<TrainingTagEntity> first, Set<TrainingTagEntity> second) {
        Set<TrainingTagEntity> result = new LinkedHashSet<>();
        if (first != null) result.addAll(first);
        if (second != null) result.addAll(second);
        return result;
    }

    private static long exerciseSetCount(TrainingExerciseEntity ex) {
        TrainingExerciseType type = ex.getType() == null ? TrainingExerciseType.FREEFORM : ex.getType();
        return switch (type) {
            case FREEFORM -> ex.getSets().size();
            case EMOM -> {
                var cfg = ex.getEmomConfig();
                if (cfg == null) yield 0;
                yield !cfg.getMinuteOverrides().isEmpty()
                        ? cfg.getMinuteOverrides().size()
                        : positive(cfg.getTotalMinutes());
            }
            case TABATA -> {
                var cfg = ex.getTabataConfig();
                if (cfg == null) yield 0;
                yield !cfg.getRoundOverrides().isEmpty()
                        ? cfg.getRoundOverrides().size()
                        : positive(cfg.getRounds());
            }
            case LADDER, STEPLADDER, PYRAMID -> {
                var cfg = ex.getNumericSeriesConfig();
                yield cfg == null ? 0 : cfg.getRows().size();
            }
            case STRAIGHT_SETS -> {
                var cfg = ex.getStraightSetsConfig();
                if (cfg == null) yield 0;
                yield !cfg.getRows().isEmpty() ? cfg.getRows().size() : positive(cfg.getSetCount());
            }
            case INTERVAL -> {
                var cfg = ex.getIntervalConfig();
                yield cfg == null ? 0 : positive(cfg.getRounds());
            }
            case STRONGFIRST_LADDER -> {
                var cfg = ex.getStrongFirstLadderConfig();
                yield cfg == null ? 0 : cfg.getRows().size();
            }
            case KB_SPORT_TIME, CARDIO -> 1;
            case CIRCUIT, AMRAP -> 0;
        };
    }

    private static long circuitStepCount(
            com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitConfigEntity cfg,
            Integer stepOrder) {
        if (!cfg.getRoundEntries().isEmpty()) {
            return cfg.getRoundEntries().stream()
                    .filter(e -> !e.isSkipped())
                    .filter(e -> stepOrder != null && stepOrder.equals(e.getStepOrder()))
                    .count();
        }
        return positive(cfg.getRounds());
    }

    private static long amrapStepCount(
            com.ragnarok.ragnarok_customers_training_diary.training.types.amrap.AmrapConfigEntity cfg,
            Integer stepOrder) {
        if (!cfg.getRoundEntries().isEmpty()) {
            return cfg.getRoundEntries().stream()
                    .filter(e -> !e.isSkipped())
                    .filter(e -> stepOrder != null && stepOrder.equals(e.getStepOrder()))
                    .count();
        }
        long rounds = positive(cfg.getRoundsCompleted());
        return rounds > 0 ? rounds : 1;
    }

    private static long positive(Integer value) {
        return value != null && value > 0 ? value : 0;
    }

    private static Set<String> bodyRegionLabels(AnalysisUnit unit) {
        Set<String> labels = new LinkedHashSet<>();
        for (String region : unit.bodyRegions()) {
            addBodyRegion(labels, region, null);
        }
        for (TrainingTagEntity tag : unit.tags()) {
            addBodyRegion(labels, tag.getSystemKey(), tag);
        }
        return labels;
    }

    private static void addBodyRegion(Set<String> labels, String key, TrainingTagEntity tag) {
        if (key == null && (tag == null || tag.getCategory() != TagCategory.BODY_REGION)) return;
        String effective = key != null ? key : tag.getName();
        switch (effective) {
            case SystemTag.FULL_BODY -> {
                labels.add(CatalogLabels.bodyRegion(SystemTag.FULL_BODY));
                labels.add(CatalogLabels.bodyRegion(SystemTag.UPPER_BODY));
                labels.add(CatalogLabels.bodyRegion(SystemTag.LOWER_BODY));
            }
            case SystemTag.UPPER_BODY -> labels.add(CatalogLabels.bodyRegion(SystemTag.UPPER_BODY));
            case SystemTag.LOWER_BODY -> labels.add(CatalogLabels.bodyRegion(SystemTag.LOWER_BODY));
            case SystemTag.CORE -> labels.add(CatalogLabels.bodyRegion(SystemTag.CORE));
            default -> {
                if (tag != null && tag.getCategory() == TagCategory.BODY_REGION) {
                    labels.add(tag.getName());
                } else if (key != null && !key.isBlank()) {
                    labels.add(CatalogLabels.bodyRegion(key));
                }
            }
        }
    }

    private static Set<String> movementPatternLabels(AnalysisUnit unit) {
        Set<String> labels = new LinkedHashSet<>();
        for (String pattern : unit.movementPatterns()) {
            if (pattern != null && !pattern.isBlank()) {
                labels.add(CatalogLabels.movementPattern(pattern));
            }
        }
        for (TrainingTagEntity tag : unit.tags()) {
            if (tag.getCategory() == TagCategory.MOVEMENT_PATTERN) {
                labels.add(tag.getSystemKey() != null
                        ? CatalogLabels.movementPattern(tag.getSystemKey())
                        : tag.getName());
            } else if (isSystemMovementPatternKey(tag.getSystemKey())) {
                labels.add(CatalogLabels.movementPattern(tag.getSystemKey()));
            }
        }
        return labels;
    }

    private static boolean isSystemMovementPatternKey(String key) {
        return key != null && List.of(SystemTag.CARRY, SystemTag.GAIT, SystemTag.HINGE, SystemTag.ISOMETRY,
                SystemTag.ISOMETRIC, SystemTag.LUNGE, SystemTag.OTHER, SystemTag.OTHERS, SystemTag.PLYO,
                SystemTag.PULL, SystemTag.PUSH, SystemTag.ROTATION, SystemTag.SQUAT, SystemTag.LOCOMOTION,
                SystemTag.JUMPS, SystemTag.COORDINATION).contains(key);
    }

    private static Map<String, Long> orderedBodyRegionMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put(CatalogLabels.bodyRegion(SystemTag.FULL_BODY), 0L);
        map.put(CatalogLabels.bodyRegion(SystemTag.UPPER_BODY), 0L);
        map.put(CatalogLabels.bodyRegion(SystemTag.LOWER_BODY), 0L);
        map.put(CatalogLabels.bodyRegion(SystemTag.CORE), 0L);
        return map;
    }

    private static Map<String, Long> orderedMovementPatternMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String key : List.of(SystemTag.CARRY, SystemTag.GAIT, SystemTag.HINGE, SystemTag.ISOMETRY, SystemTag.LUNGE,
                SystemTag.OTHER, SystemTag.PLYO, SystemTag.PULL, SystemTag.PUSH, SystemTag.ROTATION, SystemTag.SQUAT,
                SystemTag.LOCOMOTION, SystemTag.JUMPS, SystemTag.COORDINATION)) {
            map.put(CatalogLabels.movementPattern(key), 0L);
        }
        return map;
    }
    private static List<LabelValuePoint> nonZeroPoints(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .map(e -> new LabelValuePoint(e.getKey(), BigDecimal.valueOf(e.getValue())))
                .toList();
    }

    private static boolean hasAnyTag(Set<TrainingTagEntity> tags, java.util.Collection<Long> tagIds) {
        if (tags == null || tags.isEmpty() || tagIds == null || tagIds.isEmpty()) return false;
        for (TrainingTagEntity tag : tags) {
            if (tag.getId() != null && tagIds.contains(tag.getId())) return true;
        }
        return false;
    }

    private static BigDecimal equipmentWeight(TrainingExerciseEntity ex) {
        BigDecimal w1 = ex.getEquipmentWeightKg();
        BigDecimal w2 = ex.getEquipmentSecondWeightKg();
        if (w1 == null) return null;
        return ex.getEquipmentCount() == 2 && w2 != null ? w1.add(w2) : w1;
    }

    private static BigDecimal stepWeight(
            com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitStepEntity step,
            BigDecimal fallback) {
        BigDecimal w1 = step.getEquipmentWeightKg();
        if (w1 == null) return step.getWeightKg() != null ? step.getWeightKg() : fallback;
        return step.getEquipmentCount() == 2 && step.getEquipmentSecondWeightKg() != null
                ? w1.add(step.getEquipmentSecondWeightKg()) : w1;
    }

    private static BigDecimal amrapStepWeight(
            com.ragnarok.ragnarok_customers_training_diary.training.types.amrap.AmrapStepEntity step,
            BigDecimal fallback) {
        BigDecimal w1 = step.getEquipmentWeightKg();
        if (w1 == null) return step.getWeightKg() != null ? step.getWeightKg() : fallback;
        return step.getEquipmentCount() == 2 && step.getEquipmentSecondWeightKg() != null
                ? w1.add(step.getEquipmentSecondWeightKg()) : w1;
    }

    private static final class ExerciseStatsAccumulator {
        BigDecimal totalVolumeKg = BigDecimal.ZERO;
        long totalSets;
        long totalReps;
        long totalMeters;
        long totalSeconds;
        long cardioSeconds;
        Integer maxReps;
        BigDecimal maxWeightKg;

        void addEffort(Integer value, String unit, BigDecimal weightKg, boolean countSet) {
            if (countSet) totalSets++;
            noteWeight(weightKg);
            if (value == null || value <= 0) return;
            String u = unit == null || unit.isBlank() ? "REPS" : unit;
            switch (u) {
                case "METERS" -> totalMeters += value;
                case "SECONDS" -> totalSeconds += value;
                default -> addRepetitions(value, weightKg);
            }
        }

        void addSeconds(Integer seconds) {
            if (seconds != null && seconds > 0) {
                totalSeconds += seconds;
            }
        }

        void addCardioBlock(Integer activeSeconds, Integer distanceM, Integer repetitions) {
            totalSets++;
            if (activeSeconds != null && activeSeconds > 0) {
                totalSeconds += activeSeconds;
                cardioSeconds += activeSeconds;
            }
            if (distanceM != null && distanceM > 0) {
                totalMeters += distanceM;
            }
            if (repetitions != null && repetitions > 0) {
                addRepetitions(repetitions, null);
            }
        }

        private void addRepetitions(Integer reps, BigDecimal weightKg) {
            totalReps += reps;
            if (maxReps == null || reps > maxReps) maxReps = reps;
            if (weightKg != null) {
                totalVolumeKg = totalVolumeKg.add(weightKg.multiply(BigDecimal.valueOf(reps)));
            }
        }

        private void noteWeight(BigDecimal weightKg) {
            if (weightKg == null || weightKg.signum() == 0) return;
            if (maxWeightKg == null || weightKg.compareTo(maxWeightKg) > 0) {
                maxWeightKg = weightKg;
            }
        }
    }
    private record AnalysisUnit(
            long count,
            Set<TrainingTagEntity> tags,
            Set<String> bodyRegions,
            Set<String> movementPatterns) {}
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
            BigDecimal maxWeightKg,
            long totalMeters,
            long totalSeconds,
            long cardioSeconds
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
