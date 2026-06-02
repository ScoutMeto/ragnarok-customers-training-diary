package com.ragnarok.ragnarok_customers_training_diary.training.types;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
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
 * Opačný směr než {@link ExerciseTypeConfigMapper} — pro edit form. Vezme entitu
 * a doplní její per-type config do {@link TrainingExerciseInput}.
 */
@Component
public class ExerciseTypeConfigToInputMapper {

    public void fillInput(TrainingExerciseInput input, TrainingExerciseEntity ex) {
        if (ex.getEmomConfig() != null) {
            input.setEmom(toEmomInput(ex.getEmomConfig()));
        }
        if (ex.getTabataConfig() != null) {
            input.setTabata(toTabataInput(ex.getTabataConfig()));
        }
        if (ex.getAmrapConfig() != null) {
            input.setAmrap(toAmrapInput(ex.getAmrapConfig()));
        }
        if (ex.getCircuitConfig() != null) {
            input.setCircuit(toCircuitInput(ex.getCircuitConfig()));
        }
        if (ex.getNumericSeriesConfig() != null) {
            input.setNumericSeries(toNumericSeriesInput(ex.getNumericSeriesConfig()));
        }
        if (ex.getCompositeSetConfig() != null) {
            input.setComposite(toCompositeInput(ex.getCompositeSetConfig()));
        }
        if (ex.getStraightSetsConfig() != null) {
            input.setStraightSets(toStraightSetsInput(ex.getStraightSetsConfig()));
        }
        if (ex.getIntervalConfig() != null) {
            var e = ex.getIntervalConfig();
            var in = new com.ragnarok.ragnarok_customers_training_diary.training.dto.IntervalConfigInput();
            in.setRounds(e.getRounds());
            in.setWorkReps(e.getWorkReps());
            in.setWorkSeconds(e.getWorkSeconds());
            in.setRestSeconds(e.getRestSeconds());
            in.setWeightKg(e.getWeightKg());
            in.setNotes(e.getNotes());
            input.setInterval(in);
        }
    }

    private StraightSetsConfigInput toStraightSetsInput(StraightSetsConfigEntity e) {
        StraightSetsConfigInput in = new StraightSetsConfigInput();
        in.setSetCount(e.getSetCount());
        in.setRepsPerSet(e.getRepsPerSet());
        in.setWeightKg(e.getWeightKg());
        in.setRestSeconds(e.getRestSeconds());
        in.setNotes(e.getNotes());
        return in;
    }

    private CompositeSetConfigInput toCompositeInput(CompositeSetConfigEntity e) {
        CompositeSetConfigInput in = new CompositeSetConfigInput();
        in.setRounds(e.getRounds());
        in.setSharedWeightKg(e.getSharedWeightKg());
        in.setRestBetweenRoundsS(e.getRestBetweenRoundsS());
        in.setNotes(e.getNotes());
        for (CompositeSetStepEntity s : e.getSteps()) {
            CompositeSetConfigInput.StepInput si = new CompositeSetConfigInput.StepInput();
            si.setOrderIndex(s.getOrderIndex());
            si.setName(s.getName());
            si.setReps(s.getReps());
            si.setWeightKg(s.getWeightKg());
            si.setRestAfterSeconds(s.getRestAfterSeconds());
            si.setNote(s.getNote());
            in.getSteps().add(si);
        }
        return in;
    }

    private NumericSeriesConfigInput toNumericSeriesInput(NumericSeriesConfigEntity e) {
        NumericSeriesConfigInput in = new NumericSeriesConfigInput();
        in.setStartValue(e.getStartValue());
        in.setPeakValue(e.getPeakValue());
        in.setStepSize(e.getStepSize());
        in.setRepSequenceCsv(e.getRepSequenceCsv());
        in.setWeightKg(e.getWeightKg());
        in.setRestSecondsBetween(e.getRestSecondsBetween());
        in.setNotes(e.getNotes());
        return in;
    }

    private CircuitConfigInput toCircuitInput(CircuitConfigEntity e) {
        CircuitConfigInput in = new CircuitConfigInput();
        in.setRounds(e.getRounds());
        in.setRestBetweenRoundsS(e.getRestBetweenRoundsS());
        in.setNotes(e.getNotes());
        for (CircuitStepEntity s : e.getSteps()) {
            CircuitConfigInput.StepInput si = new CircuitConfigInput.StepInput();
            si.setOrderIndex(s.getOrderIndex());
            si.setName(s.getName());
            si.setReps(s.getReps());
            si.setDurationSeconds(s.getDurationSeconds());
            si.setWeightKg(s.getWeightKg());
            si.setRestSeconds(s.getRestSeconds());
            si.setNote(s.getNote());
            in.getSteps().add(si);
        }
        for (CircuitRoundRestEntity r : e.getRoundRests()) {
            CircuitConfigInput.RoundRestInput ri = new CircuitConfigInput.RoundRestInput();
            ri.setRoundIndex(r.getRoundIndex());
            ri.setRestSeconds(r.getRestSeconds());
            in.getRoundRests().add(ri);
        }
        return in;
    }

    private EmomConfigInput toEmomInput(EmomConfigEntity e) {
        EmomConfigInput in = new EmomConfigInput();
        in.setTotalMinutes(e.getTotalMinutes());
        in.setIntervalSeconds(e.getIntervalSeconds());
        in.setDefaultReps(e.getDefaultReps());
        in.setDefaultWeightKg(e.getDefaultWeightKg());
        in.setNotes(e.getNotes());
        for (EmomMinuteOverrideEntity o : e.getMinuteOverrides()) {
            EmomConfigInput.MinuteOverrideInput oi = new EmomConfigInput.MinuteOverrideInput();
            oi.setMinuteIndex(o.getMinuteIndex());
            oi.setReps(o.getReps());
            oi.setWeightKg(o.getWeightKg());
            oi.setNote(o.getNote());
            in.getMinuteOverrides().add(oi);
        }
        return in;
    }

    private TabataConfigInput toTabataInput(TabataConfigEntity e) {
        TabataConfigInput in = new TabataConfigInput();
        in.setRounds(e.getRounds());
        in.setWorkSeconds(e.getWorkSeconds());
        in.setRestSeconds(e.getRestSeconds());
        in.setDefaultReps(e.getDefaultReps());
        in.setDefaultWeightKg(e.getDefaultWeightKg());
        in.setNotes(e.getNotes());
        for (TabataRoundOverrideEntity o : e.getRoundOverrides()) {
            TabataConfigInput.RoundOverrideInput oi = new TabataConfigInput.RoundOverrideInput();
            oi.setRoundIndex(o.getRoundIndex());
            oi.setReps(o.getReps());
            oi.setWeightKg(o.getWeightKg());
            oi.setNote(o.getNote());
            in.getRoundOverrides().add(oi);
        }
        return in;
    }

    private AmrapConfigInput toAmrapInput(AmrapConfigEntity e) {
        AmrapConfigInput in = new AmrapConfigInput();
        in.setTimecapSeconds(e.getTimecapSeconds());
        in.setTargetRepsPerRound(e.getTargetRepsPerRound());
        in.setTargetWeightKg(e.getTargetWeightKg());
        in.setRoundsCompleted(e.getRoundsCompleted());
        in.setExtraReps(e.getExtraReps());
        in.setNotes(e.getNotes());
        return in;
    }
}
