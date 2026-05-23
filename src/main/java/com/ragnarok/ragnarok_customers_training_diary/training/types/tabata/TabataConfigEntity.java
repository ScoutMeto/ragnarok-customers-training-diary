package com.ragnarok.ragnarok_customers_training_diary.training.types.tabata;

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

/** Tabata: N kol × workSeconds práce / restSeconds pauza. Classic = 8 × 20s / 10s. */
@Entity
@Table(name = "tabata_config")
@Getter
@Setter
@NoArgsConstructor
public class TabataConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    @Column(nullable = false)
    private Integer rounds = 8;

    @Column(name = "work_seconds", nullable = false)
    private Integer workSeconds = 20;

    @Column(name = "rest_seconds", nullable = false)
    private Integer restSeconds = 10;

    @Column(name = "default_reps")
    private Integer defaultReps;

    @Column(name = "default_weight_kg", precision = 6, scale = 2)
    private BigDecimal defaultWeightKg;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "tabataConfig", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roundIndex ASC")
    private List<TabataRoundOverrideEntity> roundOverrides = new ArrayList<>();
}
