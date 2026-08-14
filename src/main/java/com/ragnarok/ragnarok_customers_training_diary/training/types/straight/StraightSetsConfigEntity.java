package com.ragnarok.ragnarok_customers_training_diary.training.types.straight;

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

@Entity
@Table(name = "straight_sets_config")
@Getter
@Setter
@NoArgsConstructor
public class StraightSetsConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    @Column(name = "set_count", nullable = false)
    private Integer setCount;

    @Column(name = "reps_per_set")
    private Integer repsPerSet;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** kolo 10: vygenerovaná (a editovatelná) tabulka setů. */
    @jakarta.persistence.OneToMany(mappedBy = "straightSetsConfig",
            cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
            fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.OrderBy("rowIndex ASC")
    private java.util.List<StraightSetsRowEntity> rows = new java.util.ArrayList<>();
}
