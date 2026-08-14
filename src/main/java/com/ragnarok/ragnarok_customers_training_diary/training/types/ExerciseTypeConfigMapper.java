package com.ragnarok.ragnarok_customers_training_diary.training.types;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.AmrapConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CircuitConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.EmomConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TabataConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.amrap.AmrapConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitRoundRestEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitStepEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.NumericSeriesConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.series.NumericSeriesConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.StraightSetsConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.straight.StraightSetsConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.emom.EmomConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.emom.EmomMinuteOverrideEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.tabata.TabataConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.tabata.TabataRoundOverrideEntity;
import org.springframework.stereotype.Component;

/**
 * Dispatcher: vezme {@link TrainingExerciseInput} (s per-type config DTO) a aplikuje ho
 * na {@link TrainingExerciseEntity} — vytvoří nebo upraví příslušnou config entitu.
 *
 * <p>Vyvolá se z {@code TrainingService.applyExercises} po nastavení základních polí
 * cviku. Pokud {@code type=FREEFORM}, nedělá nic.
 */
@Component
public class ExerciseTypeConfigMapper {

    private final com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository tagRepository;

    public ExerciseTypeConfigMapper(
            com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Aplikuje per-type config dle hodnoty {@code input.type}.
     * Stará config (pokud byla) se zruší (orphanRemoval), nová se připojí.
     */
    public void apply(TrainingExerciseEntity exercise, TrainingExerciseInput input) {
        // Vyčistíme všechny existující configs (jen ta správná se znovu nasadí)
        exercise.setEmomConfig(null);
        exercise.setTabataConfig(null);
        exercise.setAmrapConfig(null);
        exercise.setCircuitConfig(null);
        exercise.setNumericSeriesConfig(null);
        exercise.setStraightSetsConfig(null);
        exercise.setIntervalConfig(null);
        exercise.setStrongFirstLadderConfig(null);
        exercise.setKbSportConfig(null);
        exercise.setCardioConfig(null);

        TrainingExerciseType type = input.getType();
        if (type == null) return;

        switch (type) {
            case EMOM -> applyEmom(exercise, input.getEmom());
            case TABATA -> applyTabata(exercise, input.getTabata());
            case AMRAP -> applyAmrap(exercise, input.getAmrap());
            case CIRCUIT -> applyCircuit(exercise, input.getCircuit());
            case LADDER, STEPLADDER, PYRAMID -> applyNumericSeries(exercise, input.getNumericSeries());
            case STRAIGHT_SETS -> applyStraightSets(exercise, input.getStraightSets());
            case INTERVAL -> applyInterval(exercise, input.getInterval());
            case STRONGFIRST_LADDER -> applyStrongFirstLadder(exercise, input.getStrongFirstLadder());
            case KB_SPORT_TIME -> applyKbSport(exercise, input.getKbSport());
            case CARDIO -> applyCardio(exercise, input.getCardio());
            case FREEFORM -> { /* žádný extra config — jen set tabulka */ }
        }
    }

    private void applyKbSport(TrainingExerciseEntity exercise,
                              com.ragnarok.ragnarok_customers_training_diary.training.dto.KbSportConfigInput in) {
        if (in == null) return;
        int min = in.getTotalMinutes() != null ? in.getTotalMinutes() : 0;
        int sec = in.getTotalSeconds() != null ? in.getTotalSeconds() : 0;
        int total = min * 60 + sec;
        if (total <= 0) return;

        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.types.kbsport.KbSportConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setTotalSeconds(total);
        cfg.setTotalReps(in.getTotalReps());
        Integer split = in.getSplitIntervalSeconds();
        cfg.setSplitIntervalSeconds(split != null && split > 0 ? split : null);
        cfg.setWeightKg(in.getWeightKg());
        cfg.setUnilateral(in.isUnilateral());
        cfg.setNotes(in.getNotes());

        if (in.getIntervals() != null) {
            int idx = 0;
            for (var ii : in.getIntervals()) {
                // ScoutMeto kolo 8: část má i trvání (min+s → sekundy)
                Integer duration = null;
                if (ii.getDurationMinutes() != null || ii.getDurationSeconds() != null) {
                    int dm = ii.getDurationMinutes() != null ? ii.getDurationMinutes() : 0;
                    int ds = ii.getDurationSeconds() != null ? ii.getDurationSeconds() : 0;
                    duration = dm * 60 + ds > 0 ? dm * 60 + ds : null;
                }
                boolean empty = ii.getReps() == null
                        && duration == null
                        && (ii.getNote() == null || ii.getNote().isBlank())
                        && (ii.getSide() == null || ii.getSide().isBlank());
                if (empty) continue;
                var interval = new com.ragnarok.ragnarok_customers_training_diary.training.types.kbsport.KbSportIntervalEntity();
                interval.setKbSportConfig(cfg);
                interval.setIntervalIndex(ii.getIntervalIndex() != null ? ii.getIntervalIndex() : idx);
                interval.setReps(ii.getReps());
                interval.setDurationSeconds(duration);
                interval.setSide(normalizeSide(ii.getSide()));
                interval.setNote(ii.getNote());
                cfg.getIntervals().add(interval);
                idx++;
            }
        }
        exercise.setKbSportConfig(cfg);
    }

    private String normalizeSide(String side) {
        if (side == null || side.isBlank()) return null;
        String s = side.trim().toUpperCase();
        return (s.equals("L") || s.equals("P")) ? s : null;
    }

    private void applyStrongFirstLadder(TrainingExerciseEntity exercise,
                                        com.ragnarok.ragnarok_customers_training_diary.training.dto.StrongFirstLadderConfigInput in) {
        if (in == null || in.getLadderHeight() == null) return;
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.types.strongfirst.StrongFirstLadderConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setLadderHeight(in.getLadderHeight());
        cfg.setCycles(in.getCycles() != null ? in.getCycles() : 1);
        cfg.setRestSeconds(in.getRestSeconds());
        cfg.setWeightKg(in.getWeightKg());
        cfg.setUnilateral(in.isUnilateral());
        // REPS se normalizuje na NULL (výchozí jednotka)
        cfg.setRepUnit(in.getRepUnit() != null && !in.getRepUnit().isBlank()
                && !"REPS".equals(in.getRepUnit()) ? in.getRepUnit() : null);
        cfg.setNotes(in.getNotes());

        if (in.getRows() != null) {
            int idx = 0;
            for (var r : in.getRows()) {
                if (r.getRung() == null) continue;
                var row = new com.ragnarok.ragnarok_customers_training_diary.training.types.strongfirst
                        .StrongFirstLadderRowEntity();
                row.setConfig(cfg);
                row.setRowIndex(idx++);
                row.setLadderIndex(r.getLadderIndex() != null ? r.getLadderIndex() : 1);
                row.setRung(r.getRung());
                row.setValue(r.getValue());
                row.setWeightKg(r.getWeightKg());
                row.setRestSeconds(r.getRestSeconds());
                row.setSide("L".equals(r.getSide()) || "P".equals(r.getSide()) ? r.getSide() : null);
                cfg.getRows().add(row);
            }
        }

        exercise.setStrongFirstLadderConfig(cfg);
    }

    private void applyInterval(TrainingExerciseEntity exercise,
                               com.ragnarok.ragnarok_customers_training_diary.training.dto.IntervalConfigInput in) {
        if (in == null || in.getRounds() == null) return;
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.types.interval.IntervalConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setRounds(in.getRounds());
        cfg.setWorkReps(in.getWorkReps());
        cfg.setWorkSeconds(CircuitConfigInput.toSeconds(
                in.getWorkMin(), in.getWorkSec(), in.getWorkSeconds()));
        Integer rest = CircuitConfigInput.toSeconds(
                in.getRestMin(), in.getRestSec(), in.getRestSeconds());
        cfg.setRestSeconds(rest != null ? rest : 0);
        cfg.setWeightKg(in.getWeightKg());
        cfg.setNotes(in.getNotes());
        exercise.setIntervalConfig(cfg);
    }

    private void applyStraightSets(TrainingExerciseEntity exercise, StraightSetsConfigInput in) {
        if (in == null || in.getSetCount() == null) return;

        StraightSetsConfigEntity cfg = new StraightSetsConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setSetCount(in.getSetCount());
        cfg.setRepsPerSet(in.getRepsPerSet());
        cfg.setWeightKg(in.getWeightKg());
        // kolo 10: pauza přichází jako minuty + sekundy
        cfg.setRestSeconds(CircuitConfigInput.toSeconds(
                in.getRestMin(), in.getRestSec(), in.getRestSeconds()));
        cfg.setNotes(in.getNotes());

        if (in.getRows() != null) {
            int idx = 0;
            for (StraightSetsConfigInput.RowInput r : in.getRows()) {
                if (r.getReps() == null && r.getWeightKg() == null && r.getRestSeconds() == null) continue;
                var row = new com.ragnarok.ragnarok_customers_training_diary.training.types.straight
                        .StraightSetsRowEntity();
                row.setStraightSetsConfig(cfg);
                row.setRowIndex(idx++);
                row.setReps(r.getReps());
                row.setWeightKg(r.getWeightKg());
                row.setRestSeconds(r.getRestSeconds());
                cfg.getRows().add(row);
            }
        }

        exercise.setStraightSetsConfig(cfg);
    }

    // ----- EMOM -----

    private void applyEmom(TrainingExerciseEntity exercise, EmomConfigInput in) {
        if (in == null || in.getTotalMinutes() == null) return;

        EmomConfigEntity cfg = new EmomConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setTotalMinutes(in.getTotalMinutes());
        cfg.setIntervalSeconds(in.getIntervalSeconds() != null ? in.getIntervalSeconds() : 60);
        cfg.setDefaultReps(in.getDefaultReps());
        cfg.setDefaultWeightKg(in.getDefaultWeightKg());
        cfg.setNotes(in.getNotes());

        if (in.getMinuteOverrides() != null) {
            for (EmomConfigInput.MinuteOverrideInput o : in.getMinuteOverrides()) {
                if (isEmomOverrideEmpty(o)) continue;
                EmomMinuteOverrideEntity override = new EmomMinuteOverrideEntity();
                override.setEmomConfig(cfg);
                override.setMinuteIndex(o.getMinuteIndex());
                override.setReps(o.getReps());
                override.setWeightKg(o.getWeightKg());
                override.setNote(o.getNote());
                cfg.getMinuteOverrides().add(override);
            }
        }

        exercise.setEmomConfig(cfg);
    }

    private boolean isEmomOverrideEmpty(EmomConfigInput.MinuteOverrideInput o) {
        return o.getMinuteIndex() == null
                || (o.getReps() == null && o.getWeightKg() == null
                        && (o.getNote() == null || o.getNote().isBlank()));
    }

    // ----- TABATA -----

    private void applyTabata(TrainingExerciseEntity exercise, TabataConfigInput in) {
        if (in == null || in.getRounds() == null) return;

        TabataConfigEntity cfg = new TabataConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setRounds(in.getRounds());
        cfg.setWorkSeconds(in.getWorkSeconds() != null ? in.getWorkSeconds() : 20);
        cfg.setRestSeconds(in.getRestSeconds() != null ? in.getRestSeconds() : 10);
        cfg.setDefaultReps(in.getDefaultReps());
        cfg.setDefaultWeightKg(in.getDefaultWeightKg());
        cfg.setNotes(in.getNotes());

        if (in.getRoundOverrides() != null) {
            for (TabataConfigInput.RoundOverrideInput o : in.getRoundOverrides()) {
                if (isTabataOverrideEmpty(o)) continue;
                TabataRoundOverrideEntity override = new TabataRoundOverrideEntity();
                override.setTabataConfig(cfg);
                override.setRoundIndex(o.getRoundIndex());
                override.setReps(o.getReps());
                override.setWeightKg(o.getWeightKg());
                override.setNote(o.getNote());
                cfg.getRoundOverrides().add(override);
            }
        }

        exercise.setTabataConfig(cfg);
    }

    private boolean isTabataOverrideEmpty(TabataConfigInput.RoundOverrideInput o) {
        return o.getRoundIndex() == null
                || (o.getReps() == null && o.getWeightKg() == null
                        && (o.getNote() == null || o.getNote().isBlank()));
    }

    // ----- AMRAP -----

    private void applyAmrap(TrainingExerciseEntity exercise, AmrapConfigInput in) {
        if (in == null) return;
        // kolo 10: délka AMRAPu přichází jako minuty + sekundy
        Integer timecap = CircuitConfigInput.toSeconds(
                in.getTimecapMin(), in.getTimecapSec(), in.getTimecapSeconds());
        if (timecap == null || timecap <= 0) return;

        AmrapConfigEntity cfg = new AmrapConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setTimecapSeconds(timecap);
        cfg.setTargetRepsPerRound(in.getTargetRepsPerRound());
        cfg.setTargetWeightKg(in.getTargetWeightKg());
        cfg.setRoundsCompleted(in.getRoundsCompleted());
        cfg.setExtraReps(in.getExtraReps());
        cfg.setNotes(in.getNotes());

        if (in.getSteps() != null) {
            int idx = 0;
            for (AmrapConfigInput.StepInput s : in.getSteps()) {
                if (s.getName() == null || s.getName().isBlank()) continue;
                var step = new com.ragnarok.ragnarok_customers_training_diary.training.types.amrap
                        .AmrapStepEntity();
                step.setAmrapConfig(cfg);
                step.setOrderIndex(idx++);
                step.setName(s.getName().trim());
                step.setReps(s.getReps());
                // REPS se normalizuje na NULL (výchozí jednotka)
                step.setRepUnit(s.getRepUnit() != null && !s.getRepUnit().isBlank()
                        && !"REPS".equals(s.getRepUnit()) ? s.getRepUnit() : null);
                step.setWeightKg(s.getWeightKg());
                step.setNote(s.getNote());
                step.setEquipmentName(s.getEquipmentName() != null && !s.getEquipmentName().isBlank()
                        ? s.getEquipmentName().trim() : null);
                step.setEquipmentWeightKg(s.getEquipmentWeightKg());
                step.setEquipmentCount(s.getEquipmentCount() != null && s.getEquipmentCount() == 2 ? 2 : 1);
                step.setEquipmentSecondWeightKg(
                        step.getEquipmentCount() == 2 ? s.getEquipmentSecondWeightKg() : null);
                if (s.getTagIds() != null) {
                    for (Long tagId : s.getTagIds()) {
                        tagRepository.findById(tagId).ifPresent(step.getTags()::add);
                    }
                }
                cfg.getSteps().add(step);
            }
        }

        if (in.getRoundEntries() != null) {
            int stepCount = cfg.getSteps().size();
            for (AmrapConfigInput.RoundEntryInput r : in.getRoundEntries()) {
                if (r.getRoundIndex() == null || r.getStepOrder() == null) continue;
                // řádky pro mezitím smazané cviky zahodíme
                if (r.getStepOrder() >= stepCount) continue;
                var entry = new com.ragnarok.ragnarok_customers_training_diary.training.types.amrap
                        .AmrapRoundEntryEntity();
                entry.setAmrapConfig(cfg);
                entry.setRoundIndex(r.getRoundIndex());
                entry.setStepOrder(r.getStepOrder());
                entry.setSkipped(r.isSkipped());
                entry.setActualReps(r.getActualReps());
                entry.setActualWeightKg(r.getActualWeightKg());
                entry.setNote(r.getNote());
                cfg.getRoundEntries().add(entry);
            }
        }

        exercise.setAmrapConfig(cfg);
    }

    // ----- CIRCUIT -----

    private void applyCircuit(TrainingExerciseEntity exercise, CircuitConfigInput in) {
        if (in == null || in.getRounds() == null) return;

        CircuitConfigEntity cfg = new CircuitConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setMode(in.getMode() != null ? in.getMode() : "CIRCUIT");
        cfg.setRounds(in.getRounds());
        // kolo 10: pauza mezi koly přichází jako minuty + sekundy
        cfg.setRestBetweenRoundsS(CircuitConfigInput.toSeconds(
                in.getRestBetweenRoundsMin(), in.getRestBetweenRoundsSec(), in.getRestBetweenRoundsS()));
        cfg.setNotes(in.getNotes());

        if (in.getSteps() != null) {
            int idx = 0;
            for (CircuitConfigInput.StepInput s : in.getSteps()) {
                if (s.getName() == null || s.getName().isBlank()) continue;
                CircuitStepEntity step = new CircuitStepEntity();
                step.setCircuitConfig(cfg);
                step.setOrderIndex(idx++);
                step.setName(s.getName().trim());
                step.setReps(s.getReps());
                step.setDurationSeconds(s.getDurationSeconds());
                step.setWeightKg(s.getWeightKg());
                step.setRestSeconds(s.getRestSeconds());
                // ScoutMeto kolo 9: jednotka Opakování (REPS se normalizuje na NULL)
                step.setRepUnit(s.getRepUnit() != null && !s.getRepUnit().isBlank()
                        && !"REPS".equals(s.getRepUnit()) ? s.getRepUnit() : null);
                step.setNote(s.getNote());
                // ScoutMeto kolo 8: náčiní + tagy per cvik kruhového tréninku
                step.setEquipmentName(s.getEquipmentName() != null && !s.getEquipmentName().isBlank()
                        ? s.getEquipmentName().trim() : null);
                step.setEquipmentWeightKg(s.getEquipmentWeightKg());
                step.setEquipmentCount(s.getEquipmentCount() != null && s.getEquipmentCount() == 2 ? 2 : 1);
                step.setEquipmentSecondWeightKg(
                        step.getEquipmentCount() == 2 ? s.getEquipmentSecondWeightKg() : null);
                if (s.getTagIds() != null) {
                    for (Long tagId : s.getTagIds()) {
                        tagRepository.findById(tagId).ifPresent(step.getTags()::add);
                    }
                }
                cfg.getSteps().add(step);
            }
        }

        if (in.getRoundRests() != null) {
            for (CircuitConfigInput.RoundRestInput r : in.getRoundRests()) {
                if (r.getRoundIndex() == null) continue;
                Integer sec = CircuitConfigInput.toSeconds(r.getRestMin(), r.getRestSec(), r.getRestSeconds());
                if (sec == null) continue;
                CircuitRoundRestEntity rr = new CircuitRoundRestEntity();
                rr.setCircuitConfig(cfg);
                rr.setRoundIndex(r.getRoundIndex());
                rr.setRestSeconds(sec);
                cfg.getRoundRests().add(rr);
            }
        }

        exercise.setCircuitConfig(cfg);
    }


    // ----- CARDIO (kolo 10) -----

    /**
     * Doba trvání je jediný povinný údaj CARDIA. Když ji uživatel nevyplní, dopočítá se
     * z časů tréninkové jednotky (začátek–konec); pokud ani ty nejsou, uložení selže.
     */
    private void applyCardio(TrainingExerciseEntity exercise,
                             com.ragnarok.ragnarok_customers_training_diary.training.dto.CardioConfigInput in) {
        Integer elapsed = in == null ? null : com.ragnarok.ragnarok_customers_training_diary.training.dto
                .CardioConfigInput.toSeconds(in.getElapsedHours(), in.getElapsedMin(), in.getElapsedSec());

        if (elapsed == null || elapsed <= 0) {
            elapsed = durationFromTrainingTimes(exercise);
        }
        if (elapsed == null || elapsed <= 0) {
            throw new IllegalArgumentException(
                    "U dlouhého kardia musí být vyplněná doba trvání — buď přímo u aktivity, "
                            + "nebo časem začátku a konce tréninku.");
        }

        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.types.cardio.CardioConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setElapsedSeconds(elapsed);

        if (in != null) {
            cfg.setActiveSeconds(com.ragnarok.ragnarok_customers_training_diary.training.dto
                    .CardioConfigInput.toSeconds(in.getActiveHours(), in.getActiveMin(), in.getActiveSec()));
            cfg.setDistanceM(toMeters(in.getDistance(), in.getDistanceUnit()));
            cfg.setRepetitions(in.getRepetitions());
            cfg.setSteps(in.getSteps());
            cfg.setElevationGainM(in.getElevationGainM());
            cfg.setAvgSpeedKmh(in.getAvgSpeedKmh());
            Integer pace = com.ragnarok.ragnarok_customers_training_diary.training.dto
                    .CardioConfigInput.toSeconds(null, in.getAvgPaceMin(), in.getAvgPaceSec());
            cfg.setAvgPaceSPerKm(pace != null && pace > 0 ? pace : null);
            cfg.setNotes(in.getNotes());

            if (in.getPauses() != null) {
                int idx = 0;
                for (var p : in.getPauses()) {
                    Integer duration = com.ragnarok.ragnarok_customers_training_diary.training.dto
                            .CardioConfigInput.toSeconds(null, p.getDurationMin(), p.getDurationSec());
                    if (duration == null || duration <= 0) continue;
                    Integer start = com.ragnarok.ragnarok_customers_training_diary.training.dto
                            .CardioConfigInput.toSeconds(p.getStartHours(), p.getStartMin(), p.getStartSec());
                    var pause = new com.ragnarok.ragnarok_customers_training_diary.training.types.cardio
                            .CardioPauseEntity();
                    pause.setCardioConfig(cfg);
                    pause.setOrderIndex(idx++);
                    pause.setStartFromBeginS(start != null ? start : 0);
                    pause.setDurationSeconds(duration);
                    pause.setActivePause(p.isActivePause());
                    pause.setDistanceAtPauseM(toMeters(p.getDistanceAtPause(), in.getDistanceUnit()));
                    pause.setRepetitionsAtPause(p.getRepetitionsAtPause());
                    pause.setStepsAtPause(p.getStepsAtPause());
                    pause.setNote(p.getNote());
                    cfg.getPauses().add(pause);
                }
            }
        }

        exercise.setCardioConfig(cfg);
    }

    /** Vzdálenost se interně ukládá vždy v metrech. */
    private Integer toMeters(java.math.BigDecimal value, String unit) {
        if (value == null) return null;
        java.math.BigDecimal meters = "KM".equals(unit)
                ? value.multiply(java.math.BigDecimal.valueOf(1000))
                : value;
        return meters.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }

    private Integer durationFromTrainingTimes(TrainingExerciseEntity exercise) {
        var training = exercise.getTraining();
        if (training == null || training.getStartTime() == null || training.getEndTime() == null) {
            return null;
        }
        long seconds = java.time.Duration.between(training.getStartTime(), training.getEndTime()).getSeconds();
        return seconds > 0 ? (int) seconds : null;
    }

    // ----- NUMERIC SERIES (Ladder / Stepladder / Pyramid) -----

    private void applyNumericSeries(TrainingExerciseEntity exercise, NumericSeriesConfigInput in) {
        // Pokud nic není vyplněno, nepřidávej config (no-op).
        if (in == null) return;
        boolean hasAlgo = in.getPeakValue() != null;
        boolean hasCsv = in.getRepSequenceCsv() != null && !in.getRepSequenceCsv().isBlank();
        if (!hasAlgo && !hasCsv) return;

        NumericSeriesConfigEntity cfg = new NumericSeriesConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setStartValue(in.getStartValue() != null ? in.getStartValue() : 1);
        cfg.setPeakValue(in.getPeakValue());
        cfg.setStepSize(in.getStepSize() != null ? in.getStepSize() : 1);
        cfg.setRepSequenceCsv(hasCsv ? in.getRepSequenceCsv().trim() : null);
        cfg.setWeightKg(in.getWeightKg());
        cfg.setRestSecondsBetween(in.getRestSecondsBetween());
        cfg.setNotes(in.getNotes());

        // ScoutMeto kolo 8: vygenerovaná/editovaná tabulka řádků série
        if (in.getRows() != null) {
            int idx = 0;
            for (NumericSeriesConfigInput.RowInput r : in.getRows()) {
                if (r.getReps() == null && r.getWeightKg() == null) continue;
                var row = new com.ragnarok.ragnarok_customers_training_diary.training.types.series
                        .NumericSeriesRowEntity();
                row.setConfig(cfg);
                row.setRowIndex(r.getRowIndex() != null ? r.getRowIndex() : idx);
                row.setRung(r.getRung());
                row.setReps(r.getReps());
                row.setWeightKg(r.getWeightKg());
                cfg.getRows().add(row);
                idx++;
            }
        }

        exercise.setNumericSeriesConfig(cfg);
    }
}
