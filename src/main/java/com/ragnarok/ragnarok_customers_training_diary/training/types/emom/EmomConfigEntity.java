package com.ragnarok.ragnarok_customers_training_diary.training.types.emom;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Konfigurace EMOM tréninkového cviku. PK je shodné s {@code training_exercise.id}
 * (1:1 mapování přes {@link MapsId}).
 */
@Entity
@Table(name = "emom_config")
@Getter
@Setter
@NoArgsConstructor
public class EmomConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds = 60;

    @Column(name = "default_reps")
    private Integer defaultReps;

    @Column(name = "default_weight_kg", precision = 6, scale = 2)
    private BigDecimal defaultWeightKg;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "emomConfig", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("minuteIndex ASC")
    private List<EmomMinuteOverrideEntity> minuteOverrides = new ArrayList<>();
}
