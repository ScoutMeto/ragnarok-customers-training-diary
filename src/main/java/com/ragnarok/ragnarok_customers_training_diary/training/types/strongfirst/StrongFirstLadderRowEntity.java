package com.ragnarok.ragnarok_customers_training_diary.training.types.strongfirst;

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
 * Jedna série StrongFirst žebříku (kolo 10) — konkrétní příčka v konkrétním žebříku.
 *
 * <p>Tabulka se vygeneruje z vrcholu a počtu žebříků (1,2,…,vrchol × N) a uživatel
 * ji může přepsat podle toho, co reálně odjel.
 */
@Entity
@Table(name = "strongfirst_ladder_row")
@Getter
@Setter
@NoArgsConstructor
public class StrongFirstLadderRowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private StrongFirstLadderConfigEntity config;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    /** 1-based pořadí žebříku v tréninku. */
    @Column(name = "ladder_index", nullable = false)
    private Integer ladderIndex;

    /** Příčka žebříku (1, 2, 3, …). */
    @Column(nullable = false)
    private Integer rung;

    /**
     * Skutečný výkon — opakování, sekundy nebo metry podle jednotky configu.
     * Sloupec je {@code actual_value}: {@code value} je v H2 rezervované slovo.
     */
    @Column(name = "actual_value")
    private Integer value;

    @Column(name = "weight_kg", precision = 7, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    /** L / P u unilaterálních cviků, jinak {@code null}. */
    @Column(length = 1)
    private String side;
}
