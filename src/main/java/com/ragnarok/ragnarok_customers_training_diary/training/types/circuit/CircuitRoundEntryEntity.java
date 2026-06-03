package com.ragnarok.ragnarok_customers_training_diary.training.types.circuit;

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
 * Phase 12 / A3: skutečný záznam jednoho kroku v jednom kole circuitu.
 *
 * <p>Plán circuitu drží {@link CircuitStepEntity} (cviky v kole) a {@code rounds} (počet kol).
 * Tady se zaznamenává, co se reálně odehrálo — pro každou buňku (kolo × krok) lze cvik
 * vyřadit ({@link #skipped}), nahradit jiným ({@link #substituteName}) nebo zapsat skutečné
 * reps/váhu. Záznam vzniká na detailu tréninku po odcvičení.
 */
@Entity
@Table(name = "circuit_round_entry")
@Getter
@Setter
@NoArgsConstructor
public class CircuitRoundEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circuit_config_id", nullable = false)
    private CircuitConfigEntity circuitConfig;

    /** 1-based pořadí kola. */
    @Column(name = "round_index", nullable = false)
    private Integer roundIndex;

    /** = {@link CircuitStepEntity#getOrderIndex()} (0-based), na který krok se záznam váže. */
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private boolean skipped = false;

    /** Náhradní cvik pro toto kolo (NULL = dle plánu). */
    @Column(name = "substitute_name", length = 128)
    private String substituteName;

    @Column(name = "actual_reps")
    private Integer actualReps;

    @Column(name = "actual_weight_kg", precision = 7, scale = 2)
    private BigDecimal actualWeightKg;

    @Column(length = 255)
    private String note;
}
