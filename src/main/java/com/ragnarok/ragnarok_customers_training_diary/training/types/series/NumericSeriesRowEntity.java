package com.ragnarok.ragnarok_customers_training_diary.training.types.series;

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
 * ScoutMeto kolo 8: jeden řádek vygenerované (a uživatelem editovatelné) tabulky
 * série pro Ladder/Stepladder/Pyramid. {@code rung} = číslo série/stupně
 * („1. série", „2. série"…), {@code rowIndex} = pořadí řádku v celé tabulce.
 */
@Entity
@Table(name = "numeric_series_row")
@Getter
@Setter
@NoArgsConstructor
public class NumericSeriesRowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private NumericSeriesConfigEntity config;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    /** Číslo série/stupně (label „N. série"). */
    private Integer rung;

    private Integer reps;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;
}
