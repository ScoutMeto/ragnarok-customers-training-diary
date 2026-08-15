package com.ragnarok.ragnarok_customers_training_diary.analysis;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogLabels;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Výkonnostní statistiky (ScoutMeto kolo 10).
 *
 * <p>Na rozdíl od {@link AnalysisService}, který agreguje přes JPQL nad tabulkou setů,
 * tahle služba načte tréninky za období a metriky spočítá v Javě. Důvod: výkon už
 * neleží jen v {@code exercise_set} — je rozprostřený do deseti per-type configů
 * (kroky kruhového tréninku, kola AMRAPu, řádky sérií a žebříků, KB sport, interval,
 * kardio). Posbírat to dvaceti JPQL dotazy by bylo výrazně křehčí než jedním průchodem
 * v paměti; při velikosti jedné tělocvičny je to bez debat rychlejší než psát to znovu.
 *
 * <p><b>Charakter provedení</b> se určuje podle systémových tagů cviku (klíč, ne název):
 * Izometrie → výdrž v sekundách, Nošení → metry nebo sekundy, jinak repetitivní
 * provedení (série / opakování / nazvedané kg). Cvik bez tagu se počítá jako repetitivní.
 */
@Service
@Transactional(readOnly = true)
public class PerformanceAnalysisService {

    /** Charakter provedení cviku — rozhoduje, které metriky dávají smysl. */
    public enum Character { REPETITIVE, CARRY, ISOMETRY }

    @PersistenceContext
    private EntityManager em;

    // =========================================================================
    // Veřejné API
    // =========================================================================

    /** Souhrn za jeden cvik (podle názvu) přes celé období — P58. */
    public ExerciseSummary exerciseSummary(AccountEntity owner, String exerciseName,
                                           LocalDate from, LocalDate to) {
        Acc acc = new Acc();
        Character character = Character.REPETITIVE;
        boolean bodyweight = true;
        boolean seen = false;
        // Tělesná váha je parametr tréninkové jednotky a mezi tréninky se mění,
        // takže poměr počítáme za každý trénink zvlášť a sčítáme.
        BigDecimal ratioSum = BigDecimal.ZERO;
        boolean anyRatio = false;

        for (TrainingEntity t : loadTrainings(owner, from, to)) {
            Acc perTraining = new Acc();
            for (TrainingExerciseEntity ex : t.getExercises()) {
                if (!exerciseName.equals(displayName(ex))) continue;
                if (!seen) { character = characterOf(ex); seen = true; }
                bodyweight = bodyweight && isBodyweight(ex);
                collect(ex, acc);
                collect(ex, perTraining);
            }
            BigDecimal ratio = ratioToBodyweight(perTraining.liftedKg, t.getBodyweightKg());
            if (ratio != null && ratio.signum() > 0) {
                ratioSum = ratioSum.add(ratio);
                anyRatio = true;
            }
        }
        return new ExerciseSummary(exerciseName, character.name(), bodyweight,
                acc.sets, acc.reps, acc.liftedKg, acc.meters, acc.seconds,
                acc.minWeightKg, acc.maxWeightKg,
                anyRatio ? ratioSum : null);
    }

    /** Tréninky za období se souhrnem a rozpadem na cviky — P59. */
    public List<TrainingSummary> trainingSummaries(AccountEntity owner, LocalDate from, LocalDate to,
                                                   Collection<Long> requiredTrainingTagIds) {
        List<TrainingSummary> out = new ArrayList<>();
        for (TrainingEntity t : loadTrainings(owner, from, to)) {
            if (!hasAllTags(t.getTags(), requiredTrainingTagIds)) continue;

            Acc total = new Acc();
            List<ExerciseSummary> perExercise = new ArrayList<>();
            for (TrainingExerciseEntity ex : t.getExercises()) {
                Acc one = new Acc();
                collect(ex, one);
                collect(ex, total);
                perExercise.add(new ExerciseSummary(displayName(ex), characterOf(ex).name(),
                        isBodyweight(ex), one.sets, one.reps, one.liftedKg, one.meters, one.seconds,
                        one.minWeightKg, one.maxWeightKg,
                        ratioToBodyweight(one.liftedKg, t.getBodyweightKg())));
            }
            out.add(new TrainingSummary(t.getId(), t.getTrainingDate(), t.getName(),
                    t.getDifficulty() != null ? t.getDifficulty().getLabel() : null,
                    t.getBodyweightKg(),
                    new ExerciseSummary("Celkem", null, false, total.sets, total.reps, total.liftedKg,
                            total.meters, total.seconds, total.minWeightKg, total.maxWeightKg,
                            ratioToBodyweight(total.liftedKg, t.getBodyweightKg())),
                    perExercise));
        }
        return out;
    }

    /** Pět výkonnostních oblastí po jednotlivých trénincích — P60. */
    public List<AreaPoint> performanceAreas(AccountEntity owner, LocalDate from, LocalDate to) {
        List<AreaPoint> out = new ArrayList<>();
        for (TrainingEntity t : loadTrainings(owner, from, to)) {
            Areas a = new Areas();
            for (TrainingExerciseEntity ex : t.getExercises()) {
                collectAreas(ex, a);
            }
            out.add(new AreaPoint(t.getId(), t.getTrainingDate(), t.getName(),
                    a.externalWeight, a.bodyweightReps, a.timeUnderLoad, a.distanceUnderLoad,
                    a.longCardioSeconds));
        }
        return out;
    }

    /** Rozpady pro koláčové/sloupcové grafy — P61. */
    public Distributions distributions(AccountEntity owner, LocalDate from, LocalDate to) {
        Map<String, Long> byType = new LinkedHashMap<>();
        Map<String, Long> byDifficulty = new LinkedHashMap<>();
        Map<String, Long> byBodyPart = new LinkedHashMap<>();
        Map<String, Long> byLaterality = new LinkedHashMap<>();
        Map<String, Long> byLoadKind = new LinkedHashMap<>();
        Map<String, Long> byIsolation = new LinkedHashMap<>();
        Map<String, Long> byCharacter = new LinkedHashMap<>();
        Map<String, Long> byTag = new LinkedHashMap<>();
        // konkrétní čísla k charakteru provedení (P.O. / kg / m / s)
        Acc repetitive = new Acc();
        Acc carry = new Acc();
        Acc isometry = new Acc();

        for (TrainingEntity t : loadTrainings(owner, from, to)) {
            if (t.getDifficulty() != null) {
                bump(byDifficulty, t.getDifficulty().getLabel());
            }
            for (TrainingExerciseEntity ex : t.getExercises()) {
                bump(byType, ex.getType() != null ? ex.getType().name() : "—");

                if (ex.getType() == TrainingExerciseType.CIRCUIT && ex.getCircuitConfig() != null) {
                    for (var step : ex.getCircuitConfig().getSteps()) {
                        Set<TrainingTagEntity> tags = mergeTags(ex.getTags(), step.getTags());
                        addTagDistribution(tags, byTag, byBodyPart, byLaterality, byLoadKind, byIsolation, byCharacter,
                                isBodyweightStep(tags, stepWeight(step, equipmentWeight(ex))));
                    }
                    collect(ex, switch (characterOf(ex)) {
                        case REPETITIVE -> repetitive;
                        case CARRY -> carry;
                        case ISOMETRY -> isometry;
                    });
                    continue;
                }

                if (ex.getType() == TrainingExerciseType.AMRAP && ex.getAmrapConfig() != null) {
                    for (var step : ex.getAmrapConfig().getSteps()) {
                        Set<TrainingTagEntity> tags = mergeTags(ex.getTags(), step.getTags());
                        addTagDistribution(tags, byTag, byBodyPart, byLaterality, byLoadKind, byIsolation, byCharacter,
                                isBodyweightStep(tags, amrapStepWeight(step, equipmentWeight(ex))));
                    }
                    collect(ex, switch (characterOf(ex)) {
                        case REPETITIVE -> repetitive;
                        case CARRY -> carry;
                        case ISOMETRY -> isometry;
                    });
                    continue;
                }

                addTagDistribution(ex.getTags(), byTag, byBodyPart, byLaterality, byLoadKind, byIsolation, byCharacter,
                        isBodyweight(ex));
                Character ch = characterOf(ex);
                collect(ex, switch (ch) {
                    case REPETITIVE -> repetitive;
                    case CARRY -> carry;
                    case ISOMETRY -> isometry;
                });
            }
        }
        return new Distributions(byType, byDifficulty, byBodyPart, byLaterality, byLoadKind,
                byIsolation, byCharacter, byTag,
                repetitive.reps, repetitive.liftedKg, carry.meters, carry.seconds, isometry.seconds);
    }

    /** Sledované proměnné v čase — P63. */
    public List<VariablePoint> variableTrends(AccountEntity owner, LocalDate from, LocalDate to) {
        List<VariablePoint> out = new ArrayList<>();
        for (TrainingEntity t : loadTrainings(owner, from, to)) {
            Areas a = new Areas();
            List<Short> exerciseRpes = new ArrayList<>();
            for (TrainingExerciseEntity ex : t.getExercises()) {
                collectAreas(ex, a);
                if (ex.getRpe() != null) exerciseRpes.add(ex.getRpe());
            }
            Double avgExerciseRpe = exerciseRpes.isEmpty() ? null
                    : exerciseRpes.stream().mapToInt(Short::intValue).average().orElse(0);

            out.add(new VariablePoint(t.getId(), t.getTrainingDate(),
                    avgExerciseRpe, t.getRpe(), t.getRestingHrBpm(), t.getAvgHrBpm(), t.getMaxHrBpm(),
                    t.getBodyweightKg(), t.getSleepQualityRpe(), t.getSleepQuality(),
                    a.longCardioSeconds, a.externalWeight, a.bodyweightReps,
                    t.getCyclePhase() != null ? t.getCyclePhase().name().substring(0, 1) : null));
        }
        return out;
    }

    // =========================================================================
    // Sběr metrik
    // =========================================================================

    /** Jeden „záznam výkonu": hodnota v dané jednotce + váha, pod kterou se odehrál. */
    private void addEffort(Acc acc, Integer value, String unit, BigDecimal weightKg) {
        if (value == null || value <= 0) {
            if (weightKg != null) acc.noteWeight(weightKg);
            return;
        }
        acc.sets++;
        acc.noteWeight(weightKg);
        String u = unit == null || unit.isBlank() ? "REPS" : unit;
        switch (u) {
            case "METERS" -> acc.meters += value;
            case "SECONDS" -> acc.seconds += value;
            default -> {
                acc.reps += value;
                if (weightKg != null) {
                    acc.liftedKg = acc.liftedKg.add(weightKg.multiply(BigDecimal.valueOf(value)));
                }
            }
        }
    }

    private void collect(TrainingExerciseEntity ex, Acc acc) {
        BigDecimal eqWeight = equipmentWeight(ex);
        String exUnit = ex.getSetUnit();

        switch (ex.getType() == null ? TrainingExerciseType.FREEFORM : ex.getType()) {
            case FREEFORM -> ex.getSets().forEach(s ->
                    addEffort(acc, s.getReps(), exUnit, s.getWeightKg() != null ? s.getWeightKg() : eqWeight));

            case EMOM -> {
                var cfg = ex.getEmomConfig();
                if (cfg == null) break;
                if (!cfg.getMinuteOverrides().isEmpty()) {
                    cfg.getMinuteOverrides().forEach(m ->
                            addEffort(acc, m.getReps(), exUnit,
                                    m.getWeightKg() != null ? m.getWeightKg() : eqWeight));
                } else if (cfg.getTotalMinutes() != null && cfg.getDefaultReps() != null) {
                    for (int i = 0; i < cfg.getTotalMinutes(); i++) {
                        addEffort(acc, cfg.getDefaultReps(), exUnit,
                                cfg.getDefaultWeightKg() != null ? cfg.getDefaultWeightKg() : eqWeight);
                    }
                }
            }

            case TABATA -> {
                var cfg = ex.getTabataConfig();
                if (cfg == null) break;
                cfg.getRoundOverrides().forEach(r ->
                        addEffort(acc, r.getReps(), exUnit,
                                r.getWeightKg() != null ? r.getWeightKg() : eqWeight));
            }

            case CIRCUIT -> {
                var cfg = ex.getCircuitConfig();
                if (cfg == null) break;
                if (!cfg.getRoundEntries().isEmpty()) {
                    cfg.getRoundEntries().stream().filter(e -> !e.isSkipped()).forEach(e -> {
                        var step = stepAt(cfg.getSteps(), e.getStepOrder());
                        addEffort(acc, e.getActualReps(), step != null ? step.getRepUnit() : exUnit,
                                e.getActualWeightKg() != null ? e.getActualWeightKg()
                                        : (step != null ? stepWeight(step, eqWeight) : eqWeight));
                    });
                } else {
                    int rounds = cfg.getRounds() != null ? cfg.getRounds() : 1;
                    for (int r = 0; r < rounds; r++) {
                        cfg.getSteps().forEach(step ->
                                addEffort(acc, step.getReps(), step.getRepUnit(), stepWeight(step, eqWeight)));
                    }
                }
            }

            case AMRAP -> {
                var cfg = ex.getAmrapConfig();
                if (cfg == null) break;
                if (!cfg.getRoundEntries().isEmpty()) {
                    cfg.getRoundEntries().stream().filter(e -> !e.isSkipped()).forEach(e -> {
                        var step = cfg.getSteps().size() > e.getStepOrder()
                                ? cfg.getSteps().get(e.getStepOrder()) : null;
                        addEffort(acc, e.getActualReps(), step != null ? step.getRepUnit() : exUnit,
                                e.getActualWeightKg() != null ? e.getActualWeightKg()
                                        : (step != null ? amrapStepWeight(step, eqWeight) : eqWeight));
                    });
                } else {
                    int rounds = cfg.getRoundsCompleted() != null ? cfg.getRoundsCompleted() : 0;
                    for (int r = 0; r < rounds; r++) {
                        cfg.getSteps().forEach(step ->
                                addEffort(acc, step.getReps(), step.getRepUnit(),
                                        amrapStepWeight(step, eqWeight)));
                    }
                }
            }

            case LADDER, STEPLADDER, PYRAMID -> {
                var cfg = ex.getNumericSeriesConfig();
                if (cfg == null) break;
                cfg.getRows().forEach(row ->
                        addEffort(acc, row.getReps(), exUnit,
                                row.getWeightKg() != null ? row.getWeightKg()
                                        : (cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight)));
            }

            case STRAIGHT_SETS -> {
                var cfg = ex.getStraightSetsConfig();
                if (cfg == null) break;
                if (!cfg.getRows().isEmpty()) {
                    cfg.getRows().forEach(row ->
                            addEffort(acc, row.getReps(), exUnit,
                                    row.getWeightKg() != null ? row.getWeightKg()
                                            : (cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight)));
                } else if (cfg.getSetCount() != null && cfg.getRepsPerSet() != null) {
                    for (int i = 0; i < cfg.getSetCount(); i++) {
                        addEffort(acc, cfg.getRepsPerSet(), exUnit,
                                cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight);
                    }
                }
            }

            case INTERVAL -> {
                var cfg = ex.getIntervalConfig();
                if (cfg == null) break;
                int rounds = cfg.getRounds() != null ? cfg.getRounds() : 0;
                BigDecimal w = cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight;
                for (int i = 0; i < rounds; i++) {
                    if (cfg.getWorkReps() != null && cfg.getWorkReps() > 0) {
                        addEffort(acc, cfg.getWorkReps(), exUnit, w);
                    } else {
                        addEffort(acc, cfg.getWorkSeconds(), "SECONDS", w);
                    }
                }
            }

            case STRONGFIRST_LADDER -> {
                var cfg = ex.getStrongFirstLadderConfig();
                if (cfg == null) break;
                cfg.getRows().forEach(row ->
                        addEffort(acc, row.getValue(), cfg.getRepUnit(),
                                row.getWeightKg() != null ? row.getWeightKg()
                                        : (cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight)));
            }

            case KB_SPORT_TIME -> {
                var cfg = ex.getKbSportConfig();
                if (cfg == null) break;
                BigDecimal w = cfg.getWeightKg() != null ? cfg.getWeightKg() : eqWeight;
                addEffort(acc, cfg.getTotalReps(), "REPS", w);
                if (cfg.getTotalSeconds() != null) acc.seconds += cfg.getTotalSeconds();
            }

            case CARDIO -> {
                var cfg = ex.getCardioConfig();
                if (cfg == null) break;
                acc.sets++;
                Integer active = cfg.resolvedActiveSeconds();
                if (active != null) acc.seconds += active;
                if (cfg.getDistanceM() != null) acc.meters += cfg.getDistanceM();
                if (cfg.getRepetitions() != null) acc.reps += cfg.getRepetitions();
            }
        }
    }

    /**
     * Pět oblastí podle zadání: externí váha (kg × opakování), vlastní váha (opakování),
     * čas pod zátěží (kg × s), vzdálenost pod zátěží (kg × m) a dlouhé kardio (čistý čas).
     * Dlouhé kardio se pozná podle TYPU cviku, ne podle tagu — „Kardio" může být i interval.
     */
    private void collectAreas(TrainingExerciseEntity ex, Areas areas) {
        if (ex.getType() == TrainingExerciseType.CARDIO) {
            var cfg = ex.getCardioConfig();
            Integer active = cfg != null ? cfg.resolvedActiveSeconds() : null;
            if (active != null) areas.longCardioSeconds += active;
            return;
        }

        Acc acc = new Acc();
        collect(ex, acc);
        BigDecimal weight = acc.maxWeightKg != null ? acc.maxWeightKg : equipmentWeight(ex);

        switch (characterOf(ex)) {
            case REPETITIVE -> {
                if (isBodyweight(ex)) {
                    areas.bodyweightReps += acc.reps;
                } else {
                    areas.externalWeight = areas.externalWeight.add(acc.liftedKg);
                }
            }
            case ISOMETRY -> areas.timeUnderLoad = areas.timeUnderLoad
                    .add(multiply(weight, acc.seconds));
            case CARRY -> {
                areas.timeUnderLoad = areas.timeUnderLoad.add(multiply(weight, acc.seconds));
                areas.distanceUnderLoad = areas.distanceUnderLoad.add(multiply(weight, acc.meters));
            }
        }
    }

    // =========================================================================
    // Pomocné
    // =========================================================================

    @SuppressWarnings("unchecked")
    private List<TrainingEntity> loadTrainings(AccountEntity owner, LocalDate from, LocalDate to) {
        return em.createQuery(
                "SELECT DISTINCT t FROM TrainingEntity t "
                        + "LEFT JOIN FETCH t.exercises "
                        + "WHERE t.owner.id = :ownerId "
                        + "  AND t.trainingDate BETWEEN :from AND :to "
                        + "ORDER BY t.trainingDate ASC")
                .setParameter("ownerId", owner.getId())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    private static String displayName(TrainingExerciseEntity ex) {
        if (ex.getCatalogItem() != null) return ex.getCatalogItem().getName();
        return ex.getCustomName() != null ? ex.getCustomName() : "—";
    }
    private static Set<TrainingTagEntity> mergeTags(Set<TrainingTagEntity> first, Set<TrainingTagEntity> second) {
        Set<TrainingTagEntity> result = new java.util.LinkedHashSet<>();
        if (first != null) result.addAll(first);
        if (second != null) result.addAll(second);
        return result;
    }

    private void addTagDistribution(
            Set<TrainingTagEntity> tags,
            Map<String, Long> byTag,
            Map<String, Long> byBodyPart,
            Map<String, Long> byLaterality,
            Map<String, Long> byLoadKind,
            Map<String, Long> byIsolation,
            Map<String, Long> byCharacter,
            boolean bodyweight) {
        Set<String> keys = systemKeys(tags);
        for (TrainingTagEntity tag : tags) {
            bump(byTag, tag.getName());
        }
        bump(byBodyPart, bodyPartOf(tags));
        bump(byLaterality, keys.contains(SystemTag.UNILATERAL)
                || keys.contains(SystemTag.UNILATERAL_LEFT)
                || keys.contains(SystemTag.UNILATERAL_RIGHT)
                ? "Unilaterální" : keys.contains(SystemTag.BILATERAL) ? "Bilaterální" : "Neurčeno");
        bump(byLoadKind, bodyweight ? "Vlastní váha" : "Náčiní");
        bump(byIsolation, keys.contains(SystemTag.ISOLATION) ? "Izolované cvičení" : "Celé tělo");
        bump(byCharacter, characterLabel(characterOf(tags)));
    }
    private static Set<String> systemKeys(Set<TrainingTagEntity> tags) {
        return tags.stream()
                .map(TrainingTagEntity::getSystemKey)
                .filter(k -> k != null)
                .collect(Collectors.toSet());
    }

    /** Charakter provedení. Cvik bez tagu se počítá jako repetitivní. */
    public Character characterOf(TrainingExerciseEntity ex) {
        return characterOf(ex.getTags());
    }

    private Character characterOf(Set<TrainingTagEntity> tags) {
        Set<String> keys = systemKeys(tags);
        if (keys.contains(SystemTag.ISOMETRY) || keys.contains(SystemTag.ISOMETRIC)) return Character.ISOMETRY;
        if (keys.contains(SystemTag.CARRY)
                || keys.contains(SystemTag.GAIT)
                || keys.contains(SystemTag.LOCOMOTION)
                || keys.contains(SystemTag.JUMPS)) return Character.CARRY;
        return Character.REPETITIVE;
    }
    private static String characterLabel(Character c) {
        return switch (c) {
            case REPETITIVE -> "Repetitivní provedení";
            case CARRY -> "Nošení";
            case ISOMETRY -> "Izometrie";
        };
    }

    /** Cvik s vlastní vahou = má tag Vlastní váha, nebo prostě nemá zadané žádné závaží. */
    private boolean isBodyweight(TrainingExerciseEntity ex) {
        if (systemKeys(ex.getTags()).contains(SystemTag.BODYWEIGHT)) return true;
        BigDecimal w = equipmentWeight(ex);
        return w == null || w.signum() == 0;
    }

    private static String bodyPartOf(TrainingExerciseEntity ex, Set<String> keys) {
        String fromTags = bodyPartOf(ex.getTags());
        if (!"Neurčeno".equals(fromTags)) return fromTags;
        if (ex.getCatalogItem() != null && !ex.getCatalogItem().getBodyRegions().isEmpty()) {
            String first = ex.getCatalogItem().getBodyRegions().iterator().next();
            return CatalogLabels.bodyRegion(first);
        }
        return "Neurčeno";
    }

    private static String bodyPartOf(Set<TrainingTagEntity> tags) {
        Set<String> keys = systemKeys(tags);
        if (keys.contains(SystemTag.FULL_BODY)) return CatalogLabels.bodyRegion(SystemTag.FULL_BODY);
        if (keys.contains(SystemTag.UPPER_BODY)) return CatalogLabels.bodyRegion(SystemTag.UPPER_BODY);
        if (keys.contains(SystemTag.LOWER_BODY)) return CatalogLabels.bodyRegion(SystemTag.LOWER_BODY);
        if (keys.contains(SystemTag.CORE)) return CatalogLabels.bodyRegion(SystemTag.CORE);
        for (TrainingTagEntity tag : tags) {
            if (tag.getCategory() == TagCategory.BODY_REGION) {
                return tag.getName();
            }
        }
        return "Neurčeno";
    }
    private boolean isBodyweightStep(Set<TrainingTagEntity> tags, BigDecimal weight) {
        if (systemKeys(tags).contains(SystemTag.BODYWEIGHT)) return true;
        return weight == null || weight.signum() == 0;
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

    private static com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitStepEntity
            stepAt(List<com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitStepEntity> steps,
                   Integer order) {
        if (order == null) return null;
        return steps.stream().filter(s -> order.equals(s.getOrderIndex())).findFirst().orElse(null);
    }

    private static BigDecimal multiply(BigDecimal weight, long amount) {
        if (weight == null || amount == 0) return BigDecimal.ZERO;
        return weight.multiply(BigDecimal.valueOf(amount));
    }

    /** Poměr nazvedaných kg k tělesné váze — bez tělesné váhy nedává smysl. */
    private static BigDecimal ratioToBodyweight(BigDecimal liftedKg, BigDecimal bodyweightKg) {
        if (liftedKg == null || bodyweightKg == null || bodyweightKg.signum() == 0) return null;
        return liftedKg.divide(bodyweightKg, 2, java.math.RoundingMode.HALF_UP);
    }

    private static boolean hasAllTags(Set<TrainingTagEntity> tags, Collection<Long> required) {
        if (required == null || required.isEmpty()) return true;
        if (tags == null || tags.isEmpty()) return false;
        Set<Long> ids = tags.stream()
                .map(TrainingTagEntity::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        return ids.containsAll(required);
    }

    private static void bump(Map<String, Long> map, String key) {
        map.merge(key, 1L, Long::sum);
    }

    /** Průběžný součet metrik. */
    private static final class Acc {
        long sets;
        long reps;
        long meters;
        long seconds;
        BigDecimal liftedKg = BigDecimal.ZERO;
        BigDecimal minWeightKg;
        BigDecimal maxWeightKg;

        void noteWeight(BigDecimal w) {
            if (w == null || w.signum() == 0) return;
            if (minWeightKg == null || w.compareTo(minWeightKg) < 0) minWeightKg = w;
            if (maxWeightKg == null || w.compareTo(maxWeightKg) > 0) maxWeightKg = w;
        }
    }

    private static final class Areas {
        BigDecimal externalWeight = BigDecimal.ZERO;
        long bodyweightReps;
        BigDecimal timeUnderLoad = BigDecimal.ZERO;
        BigDecimal distanceUnderLoad = BigDecimal.ZERO;
        long longCardioSeconds;
    }

    // =========================================================================
    // Výstupní typy
    // =========================================================================

    public record ExerciseSummary(
            String name, String character, boolean bodyweight,
            long sets, long reps, BigDecimal liftedKg, long meters, long seconds,
            BigDecimal minWeightKg, BigDecimal maxWeightKg, BigDecimal liftedPerBodyweight) {}

    public record TrainingSummary(
            Long trainingId, LocalDate date, String name, String difficultyLabel,
            BigDecimal bodyweightKg, ExerciseSummary total, List<ExerciseSummary> exercises) {}

    public record AreaPoint(
            Long trainingId, LocalDate date, String name,
            BigDecimal externalWeight, long bodyweightReps,
            BigDecimal timeUnderLoad, BigDecimal distanceUnderLoad, long longCardioSeconds) {}

    public record Distributions(
            Map<String, Long> byExerciseType,
            Map<String, Long> byDifficulty,
            Map<String, Long> byBodyPart,
            Map<String, Long> byLaterality,
            Map<String, Long> byLoadKind,
            Map<String, Long> byIsolation,
            Map<String, Long> byCharacter,
            Map<String, Long> byTag,
            long repetitiveReps, BigDecimal repetitiveKg,
            long carryMeters, long carrySeconds, long isometrySeconds) {}

    public record VariablePoint(
            Long trainingId, LocalDate date,
            Double avgExerciseRpe, Short trainingRpe,
            Short restingHrBpm, Short avgHrBpm, Short maxHrBpm,
            BigDecimal bodyweightKg, Short sleepQualityRpe, Short sleepQuality,
            long cardioSeconds, BigDecimal liftedKg, long bodyweightReps,
            String cyclePhaseLetter) {}
}
