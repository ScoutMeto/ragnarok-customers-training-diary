package com.ragnarok.ragnarok_customers_training_diary.training.types;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
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

        TrainingExerciseType type = input.getType();
        if (type == null) return;

        switch (type) {
            case EMOM -> applyEmom(exercise, input.getEmom());
            case TABATA -> applyTabata(exercise, input.getTabata());
            case AMRAP -> applyAmrap(exercise, input.getAmrap());
            case FREEFORM, CUSTOMIZING, STRAIGHT_SETS -> { /* žádný extra config */ }
            // Ostatní typy přijdou v P3.2-3.4
            default -> { /* TODO P3.2+ */ }
        }
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
}
