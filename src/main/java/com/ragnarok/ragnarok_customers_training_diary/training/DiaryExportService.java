package com.ragnarok.ragnarok_customers_training_diary.training;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ScoutMeto kolo 9: „Export deníku v json" — kompletní data deníku uživatele
 * ve strojově čitelné podobě (pro vyhodnocení libovolnou AI) + druhý JSON
 * s definicí/popisem polí ({@code diary-export-schema.json} v resources).
 */
@Service
public class DiaryExportService {

    private final TrainingRepository trainingRepository;
    private final ObjectMapper mapper;

    public DiaryExportService(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
        this.mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /** Celý deník uživatele jako JSON bytes (UTF-8). */
    @Transactional(readOnly = true)
    public byte[] exportDiary(AccountEntity owner) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", "ragnarok-training-diary");
        root.put("formatVersion", 1);
        root.put("exportedAt", LocalDateTime.now().toString());
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("nickname", owner.getNickname());
        user.put("firstName", owner.getFirstName());
        user.put("lastName", owner.getLastName());
        user.put("gender", owner.getGender() != null ? owner.getGender().name() : null);
        user.put("birthDate", owner.getBirthDate() != null ? owner.getBirthDate().toString() : null);
        root.put("user", user);

        List<Map<String, Object>> trainings = new ArrayList<>();
        for (TrainingEntity t : trainingRepository
                .findByOwner_IdAndVisibilityOrderByTrainingDateDescIdDesc(
                        owner.getId(), TrainingVisibility.PRIVATE)) {
            trainings.add(mapTraining(t));
        }
        root.put("trainings", trainings);

        try {
            return mapper.writeValueAsBytes(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Export deníku selhal při serializaci.", ex);
        }
    }

    /** Popis formátu (statický resource) — druhý soubor exportu. */
    public byte[] exportSchema() {
        try (var in = new ClassPathResource("diary-export-schema.json").getInputStream()) {
            return in.readAllBytes();
        } catch (java.io.IOException ex) {
            return "{\"error\":\"schema unavailable\"}".getBytes(StandardCharsets.UTF_8);
        }
    }

    private Map<String, Object> mapTraining(TrainingEntity t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", t.getTrainingDate() != null ? t.getTrainingDate().toString() : null);
        m.put("startTime", t.getStartTime() != null ? t.getStartTime().toString() : null);
        m.put("endTime", t.getEndTime() != null ? t.getEndTime().toString() : null);
        m.put("name", t.getName());
        m.put("difficulty", t.getDifficulty() != null ? t.getDifficulty().name() : null);
        m.put("rpe", t.getRpe());
        m.put("notes", t.getNotes());
        m.put("flagged", t.isFlagged());
        m.put("tags", tagNames(t.getTags()));
        // kondiční metriky (kolo 5)
        m.put("bodyweightKg", t.getBodyweightKg());
        m.put("restingHeartRate", t.getRestingHrBpm());
        m.put("avgHeartRate", t.getAvgHrBpm());
        m.put("maxHeartRate", t.getMaxHrBpm());
        m.put("sleepQuality", t.getSleepQuality());
        m.put("sleepRpe", t.getSleepQualityRpe());
        m.put("cycleDay", t.getCycleDay());
        m.put("cyclePhase", t.getCyclePhase() != null ? t.getCyclePhase().name() : null);

        List<Map<String, Object>> exercises = new ArrayList<>();
        for (TrainingExerciseEntity ex : t.getExercises()) {
            exercises.add(mapExercise(ex));
        }
        m.put("exercises", exercises);
        return m;
    }

    private Map<String, Object> mapExercise(TrainingExerciseEntity ex) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", ex.getOrderIndex());
        m.put("type", ex.getType() != null ? ex.getType().name() : null);
        m.put("name", ex.getCatalogItem() != null ? ex.getCatalogItem().getName() : ex.getCustomName());
        m.put("rpe", ex.getRpe());
        m.put("notes", ex.getNotes());
        m.put("tags", tagNames(ex.getTags()));
        m.put("setUnit", ex.getSetUnit());
        if (ex.getEquipmentName() != null || ex.getEquipmentWeightKg() != null) {
            Map<String, Object> eq = new LinkedHashMap<>();
            eq.put("name", ex.getEquipmentName());
            eq.put("weightKg", ex.getEquipmentWeightKg());
            eq.put("count", ex.getEquipmentCount());
            eq.put("secondWeightKg", ex.getEquipmentSecondWeightKg());
            m.put("equipment", eq);
        }
        if (!ex.getSets().isEmpty()) {
            List<Map<String, Object>> sets = new ArrayList<>();
            for (ExerciseSetEntity s : ex.getSets()) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("index", s.getSetIndex());
                sm.put("weightKg", s.getWeightKg());
                sm.put("value", s.getReps()); // jednotka dle exercise.setUnit (null = opakování)
                sm.put("restSeconds", s.getRestSeconds());
                sm.put("rpe", s.getRpe());
                sm.put("note", s.getNote());
                sets.add(sm);
            }
            m.put("sets", sets);
        }
        addConfigs(m, ex);
        return m;
    }

    private void addConfigs(Map<String, Object> m, TrainingExerciseEntity ex) {
        if (ex.getEmomConfig() != null) {
            var c = ex.getEmomConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("totalMinutes", c.getTotalMinutes());
            cm.put("defaultReps", c.getDefaultReps());
            cm.put("notes", c.getNotes());
            List<Map<String, Object>> mins = new ArrayList<>();
            c.getMinuteOverrides().forEach(o -> {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("minute", o.getMinuteIndex());
                om.put("reps", o.getReps());
                om.put("weightKg", o.getWeightKg());
                om.put("note", o.getNote());
                mins.add(om);
            });
            if (!mins.isEmpty()) cm.put("minutes", mins);
            m.put("emom", cm);
        }
        if (ex.getTabataConfig() != null) {
            var c = ex.getTabataConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("rounds", c.getRounds());
            cm.put("workSeconds", c.getWorkSeconds());
            cm.put("restSeconds", c.getRestSeconds());
            cm.put("defaultReps", c.getDefaultReps());
            cm.put("notes", c.getNotes());
            List<Map<String, Object>> rounds = new ArrayList<>();
            c.getRoundOverrides().forEach(o -> {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("round", o.getRoundIndex());
                om.put("reps", o.getReps());
                rounds.add(om);
            });
            if (!rounds.isEmpty()) cm.put("roundsDetail", rounds);
            m.put("tabata", cm);
        }
        if (ex.getAmrapConfig() != null) {
            var c = ex.getAmrapConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("timecapSeconds", c.getTimecapSeconds());
            cm.put("targetRepsPerRound", c.getTargetRepsPerRound());
            cm.put("targetWeightKg", c.getTargetWeightKg());
            cm.put("roundsCompleted", c.getRoundsCompleted());
            cm.put("extraReps", c.getExtraReps());
            cm.put("notes", c.getNotes());
            m.put("amrap", cm);
        }
        if (ex.getCircuitConfig() != null) {
            var c = ex.getCircuitConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("rounds", c.getRounds());
            cm.put("restBetweenRoundsSeconds", c.getRestBetweenRoundsS());
            cm.put("notes", c.getNotes());
            List<Map<String, Object>> steps = new ArrayList<>();
            c.getSteps().forEach(s -> {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("order", s.getOrderIndex());
                sm.put("name", s.getName());
                sm.put("value", s.getReps());
                sm.put("valueUnit", s.getRepUnit()); // null = opakování
                sm.put("durationSeconds", s.getDurationSeconds());
                sm.put("restSeconds", s.getRestSeconds());
                sm.put("note", s.getNote());
                sm.put("tags", tagNames(s.getTags()));
                if (s.getEquipmentName() != null || s.getEquipmentWeightKg() != null) {
                    Map<String, Object> eq = new LinkedHashMap<>();
                    eq.put("name", s.getEquipmentName());
                    eq.put("weightKg", s.getEquipmentWeightKg());
                    eq.put("count", s.getEquipmentCount());
                    eq.put("secondWeightKg", s.getEquipmentSecondWeightKg());
                    sm.put("equipment", eq);
                }
                steps.add(sm);
            });
            cm.put("steps", steps);
            List<Map<String, Object>> entries = new ArrayList<>();
            c.getRoundEntries().forEach(e -> {
                Map<String, Object> em = new LinkedHashMap<>();
                em.put("round", e.getRoundIndex());
                em.put("stepOrder", e.getStepOrder());
                em.put("skipped", e.isSkipped());
                em.put("substituteName", e.getSubstituteName());
                em.put("actualReps", e.getActualReps());
                em.put("actualWeightKg", e.getActualWeightKg());
                em.put("note", e.getNote());
                entries.add(em);
            });
            if (!entries.isEmpty()) cm.put("actualLog", entries);
            m.put("circuit", cm);
        }
        if (ex.getNumericSeriesConfig() != null) {
            var c = ex.getNumericSeriesConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("startValue", c.getStartValue());
            cm.put("peakValue", c.getPeakValue());
            cm.put("stepSize", c.getStepSize());
            cm.put("restSecondsBetween", c.getRestSecondsBetween());
            cm.put("notes", c.getNotes());
            List<Map<String, Object>> rows = new ArrayList<>();
            c.getRows().forEach(r -> {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("rung", r.getRung());
                rm.put("value", r.getReps()); // jednotka dle exercise.setUnit
                rm.put("weightKg", r.getWeightKg());
                rows.add(rm);
            });
            if (!rows.isEmpty()) cm.put("rows", rows);
            m.put("series", cm);
        }
        if (ex.getStraightSetsConfig() != null) {
            var c = ex.getStraightSetsConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("setCount", c.getSetCount());
            cm.put("repsPerSet", c.getRepsPerSet());
            cm.put("weightKg", c.getWeightKg());
            cm.put("restSeconds", c.getRestSeconds());
            cm.put("notes", c.getNotes());
            m.put("straightSets", cm);
        }
        if (ex.getIntervalConfig() != null) {
            var c = ex.getIntervalConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("rounds", c.getRounds());
            cm.put("workReps", c.getWorkReps());
            cm.put("workSeconds", c.getWorkSeconds());
            cm.put("restSeconds", c.getRestSeconds());
            cm.put("weightKg", c.getWeightKg());
            cm.put("notes", c.getNotes());
            m.put("interval", cm);
        }
        if (ex.getStrongFirstLadderConfig() != null) {
            var c = ex.getStrongFirstLadderConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("ladderHeight", c.getLadderHeight());
            cm.put("cycles", c.getCycles());
            cm.put("restSeconds", c.getRestSeconds());
            cm.put("weightKg", c.getWeightKg());
            cm.put("unilateral", c.isUnilateral());
            cm.put("notes", c.getNotes());
            m.put("strongFirstLadder", cm);
        }
        if (ex.getKbSportConfig() != null) {
            var c = ex.getKbSportConfig();
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("totalSeconds", c.getTotalSeconds());
            cm.put("totalReps", c.getTotalReps());
            cm.put("weightKg", c.getWeightKg());
            cm.put("unilateralHandswitch", c.isUnilateral());
            cm.put("notes", c.getNotes());
            List<Map<String, Object>> parts = new ArrayList<>();
            c.getIntervals().forEach(iv -> {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("part", iv.getIntervalIndex() + 1);
                pm.put("reps", iv.getReps());
                pm.put("durationSeconds", iv.getDurationSeconds());
                pm.put("side", iv.getSide());
                pm.put("note", iv.getNote());
                parts.add(pm);
            });
            if (!parts.isEmpty()) cm.put("parts", parts);
            m.put("kbSport", cm);
        }
    }

    private List<String> tagNames(java.util.Collection<TrainingTagEntity> tags) {
        return tags == null ? List.of()
                : tags.stream().map(TrainingTagEntity::getName).sorted().toList();
    }
}
