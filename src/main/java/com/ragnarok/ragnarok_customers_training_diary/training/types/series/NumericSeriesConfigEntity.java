package com.ragnarok.ragnarok_customers_training_diary.training.types.series;

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
 * Sdílená konfigurace pro Ladder, Stepladder a Pyramid (typ určuje
 * {@code training_exercise.type}). Buď algoritmický pattern (start/peak/step)
 * nebo custom sequence ({@code rep_sequence_csv}).
 */
@Entity
@Table(name = "numeric_series_config")
@Getter
@Setter
@NoArgsConstructor
public class NumericSeriesConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    @Column(name = "start_value")
    private Integer startValue = 1;

    @Column(name = "peak_value")
    private Integer peakValue;

    @Column(name = "step_size")
    private Integer stepSize = 1;

    /**
     * Volitelný custom rep sequence (CSV). Pokud vyplněn, má přednost před start/peak/step.
     * Příklad pro Stepladder: "1,2,3,5,8".
     */
    @Column(name = "rep_sequence_csv", length = 255)
    private String repSequenceCsv;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "rest_seconds_between")
    private Integer restSecondsBetween;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** ScoutMeto kolo 8: vygenerovaná/editovaná tabulka řádků série. */
    @jakarta.persistence.OneToMany(mappedBy = "config", cascade = jakarta.persistence.CascadeType.ALL,
            orphanRemoval = true)
    @jakarta.persistence.OrderBy("rowIndex ASC")
    private java.util.List<NumericSeriesRowEntity> rows = new java.util.ArrayList<>();
}
