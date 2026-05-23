package com.ragnarok.ragnarok_customers_training_diary.training.types;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.AmrapConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.EmomConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TabataConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.amrap.AmrapConfigEntity;
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
