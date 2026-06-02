package com.ragnarok.ragnarok_customers_training_diary.training.types.strongfirst;

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
 * Phase 13 (A10): StrongFirst žebřík. Jeden cvik, série: 1 opakování + krátká
 * (nezaznamenaná) pauza, 2 opakování + pauza, ... až {@code ladderHeight}. Po
 * dosažení vrcholu se žebřík opakuje od začátku — celkem {@code cycles} cyklů.
 * Delší pauza po dokončení žebříku ({@code restSeconds}) se zaznamenává.
 * U unilaterálních cviků ({@code unilateral=true}) se provádí pro každou stranu zvlášť.
 */
@Entity
@Table(name = "strongfirst_ladder_config")
@Getter
@Setter
@NoArgsConstructor
public class StrongFirstLadderConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    /** Vrchol žebříku — nejvyšší počet opakování ve stupni (např. 5 → 1,2,3,4,5). */
    @Column(name = "ladder_height", nullable = false)
    private Integer ladderHeight;

    /** Počet opakování celého žebříku. */
    @Column(nullable = false)
    private Integer cycles = 1;

    /** Pauza po dokončení žebříku (s) — zaznamenává se (na rozdíl od pauz mezi stupni). */
    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    /** Unilaterální cvik — žebřík se provádí pro levou i pravou stranu zvlášť. */
    @Column(nullable = false)
    private boolean unilateral = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
