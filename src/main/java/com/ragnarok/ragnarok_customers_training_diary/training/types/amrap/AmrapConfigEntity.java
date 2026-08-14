package com.ragnarok.ragnarok_customers_training_diary.training.types.amrap;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AMRAP — "kolik kol stihneš v Y minutách". Plán (target_reps, target_weight) +
 * výsledek (rounds_completed, extra_reps) v jedné entitě.
 */
@Entity
@Table(name = "amrap_config")
@Getter
@Setter
@NoArgsConstructor
public class AmrapConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    @Column(name = "timecap_seconds", nullable = false)
    private Integer timecapSeconds;

    @Column(name = "target_reps_per_round")
    private Integer targetRepsPerRound;

    @Column(name = "target_weight_kg", precision = 6, scale = 2)
    private BigDecimal targetWeightKg;

    @Column(name = "rounds_completed")
    private Integer roundsCompleted;

    @Column(name = "extra_reps")
    private Integer extraReps;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** kolo 10: sada cviků, která se v AMRAPu opakuje (jako kroky kruhového tréninku). */
    @jakarta.persistence.OneToMany(mappedBy = "amrapConfig",
            cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @jakarta.persistence.OrderBy("orderIndex ASC")
    private java.util.List<AmrapStepEntity> steps = new java.util.ArrayList<>();

    /** kolo 10: skutečný záznam po kolech včetně posledního, rozjetého. */
    @jakarta.persistence.OneToMany(mappedBy = "amrapConfig",
            cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @jakarta.persistence.OrderBy("roundIndex ASC, stepOrder ASC")
    private java.util.List<AmrapRoundEntryEntity> roundEntries = new java.util.ArrayList<>();
}
