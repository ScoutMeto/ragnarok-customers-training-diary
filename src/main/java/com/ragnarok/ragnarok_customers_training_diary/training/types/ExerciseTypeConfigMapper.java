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
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CompositeSetConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.composite.CompositeSetConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.composite.CompositeSetStepEntity;
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
        exercise.setCompositeSetConfig(null);
        exercise.setStraightSetsConfig(null);
        exercise.setIntervalConfig(null);
        exercise.setStrongFirstLadderConfig(null);

        TrainingExerciseType type = input.getType();
        if (type == null) return;

        switch (type) {
            case EMOM -> applyEmom(exercise, input.getEmom());
            case TABATA -> applyTabata(exercise, input.getTabata());
            case AMRAP -> applyAmrap(exercise, input.getAmrap());
            case CIRCUIT -> applyCircuit(exercise, input.getCircuit());
            case LADDER, STEPLADDER, PYRAMID -> applyNumericSeries(exercise, input.getNumericSeries());
            case SUPERSET, COMPLEX -> applyComposite(exercise, input.getComposite());
            case STRAIGHT_SETS -> applyStraightSets(exercise, input.getStraightSets());
            case INTERVAL -> applyInterval(exercise, input.getInterval());
            case STRONGFIRST_LADDER -> applyStrongFirstLadder(exercise, input.getStrongFirstLadder());
            case FREEFORM, CARDIO, CORE -> { /* žádný extra config — jen set tabulka */ }
        }
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
        cfg.setNotes(in.getNotes());
        exercise.setStrongFirstLadderConfig(cfg);
    }

    private void applyInterval(TrainingExerciseEntity exercise,
                               com.ragnarok.ragnarok_customers_training_diary.training.dto.IntervalConfigInput in) {
        if (in == null || in.getRounds() == null) return;
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.types.interval.IntervalConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setRounds(in.getRounds());
        cfg.setWorkReps(in.getWorkReps());
        cfg.setWorkSeconds(in.getWorkSeconds());
        cfg.setRestSeconds(in.getRestSeconds() != null ? in.getRestSeconds() : 0);
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
        cfg.setRestSeconds(in.getRestSeconds());
        cfg.setNotes(in.getNotes());

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
        if (in == null || in.getTimecapSeconds() == null) return;

        AmrapConfigEntity cfg = new AmrapConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setTimecapSeconds(in.getTimecapSeconds());
        cfg.setTargetRepsPerRound(in.getTargetRepsPerRound());
        cfg.setTargetWeightKg(in.getTargetWeightKg());
        cfg.setRoundsCompleted(in.getRoundsCompleted());
        cfg.setExtraReps(in.getExtraReps());
        cfg.setNotes(in.getNotes());

        exercise.setAmrapConfig(cfg);
    }

    // ----- CIRCUIT -----

    private void applyCircuit(TrainingExerciseEntity exercise, CircuitConfigInput in) {
        if (in == null || in.getRounds() == null) return;

        CircuitConfigEntity cfg = new CircuitConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setRounds(in.getRounds());
        cfg.setRestBetweenRoundsS(in.getRestBetweenRoundsS());
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
                step.setNote(s.getNote());
                cfg.getSteps().add(step);
            }
        }

        if (in.getRoundRests() != null) {
            for (CircuitConfigInput.RoundRestInput r : in.getRoundRests()) {
                if (r.getRoundIndex() == null || r.getRestSeconds() == null) continue;
                CircuitRoundRestEntity rr = new CircuitRoundRestEntity();
                rr.setCircuitConfig(cfg);
                rr.setRoundIndex(r.getRoundIndex());
                rr.setRestSeconds(r.getRestSeconds());
                cfg.getRoundRests().add(rr);
            }
        }

        exercise.setCircuitConfig(cfg);
    }

    // ----- COMPOSITE (Superset / Complex) -----

    private void applyComposite(TrainingExerciseEntity exercise, CompositeSetConfigInput in) {
        if (in == null || in.getRounds() == null) return;

        CompositeSetConfigEntity cfg = new CompositeSetConfigEntity();
        cfg.setTrainingExercise(exercise);
        cfg.setRounds(in.getRounds());
        cfg.setSharedWeightKg(in.getSharedWeightKg());
        cfg.setRestBetweenRoundsS(in.getRestBetweenRoundsS());
        cfg.setNotes(in.getNotes());

        if (in.getSteps() != null) {
            int idx = 0;
            for (CompositeSetConfigInput.StepInput s : in.getSteps()) {
                if (s.getName() == null || s.getName().isBlank()) continue;
                CompositeSetStepEntity step = new CompositeSetStepEntity();
                step.setCompositeConfig(cfg);
                step.setOrderIndex(idx++);
                step.setName(s.getName().trim());
                step.setReps(s.getReps());
                step.setWeightKg(s.getWeightKg());
                step.setRestAfterSeconds(s.getRestAfterSeconds());
                step.setNote(s.getNote());
                cfg.getSteps().add(step);
            }
        }

        exercise.setCompositeSetConfig(cfg);
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

        exercise.setNumericSeriesConfig(cfg);
    }
}
