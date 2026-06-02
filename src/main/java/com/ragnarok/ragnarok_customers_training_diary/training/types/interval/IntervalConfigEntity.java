package com.ragnarok.ragnarok_customers_training_diary.training.types.interval;

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
 * Phase 13 (A11): Intervalový trénink — N kol, každé = práce (počet opakování NEBO
 * čas v sekundách) + pauza. Po dokončení práce následuje pauza a hned další kolo.
 */
@Entity
@Table(name = "interval_config")
@Getter
@Setter
@NoArgsConstructor
public class IntervalConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    @Column(nullable = false)
    private Integer rounds;

    /** Práce jako počet opakování (vyplněno když není časová). */
    @Column(name = "work_reps")
    private Integer workReps;

    /** Práce jako čas v sekundách (vyplněno když není repovaná). */
    @Column(name = "work_seconds")
    private Integer workSeconds;

    @Column(name = "rest_seconds", nullable = false)
    private Integer restSeconds;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
