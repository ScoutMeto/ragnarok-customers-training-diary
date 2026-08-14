package com.ragnarok.ragnarok_customers_training_diary.training.types.straight;

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
 * Jeden set ve Straight Sets (kolo 10). Tabulka se vygeneruje z předpisu
 * (počet setů × opakování × váha × pauza) a uživatel ji může přepsat podle
 * toho, co reálně odcvičil.
 */
@Entity
@Table(name = "straight_sets_row")
@Getter
@Setter
@NoArgsConstructor
public class StraightSetsRowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "straight_sets_config_id", nullable = false)
    private StraightSetsConfigEntity straightSetsConfig;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    private Integer reps;

    @Column(name = "weight_kg", precision = 7, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "rest_seconds")
    private Integer restSeconds;
}
