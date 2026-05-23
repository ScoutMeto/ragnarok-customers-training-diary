package com.ragnarok.ragnarok_customers_training_diary.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Jednotlivá série (set) cviku. Všechna pole nepovinná — klient si píše jen to, co
 * chce sledovat (pro bodyweight cvik nemusí být váha, pro time-based ani reps atd.).
 */
@Entity
@Table(name = "exercise_set")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_exercise_id", nullable = false)
    private TrainingExerciseEntity trainingExercise;

    @Column(name = "set_index", nullable = false)
    private Integer setIndex;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    private Integer reps;

    private Short rpe;

    @Column(length = 255)
    private String note;
}
