package com.ragnarok.ragnarok_customers_training_diary.training.types.amrap;

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
 * Skutečný záznam jednoho cviku v jednom kole AMRAPu (kolo 10).
 *
 * <p>Poslední kolo bývá rozjeté — cvik, na který už nezbyl čas, se označí
 * {@link #skipped}; u rozcvičeného se zapíše, kolik se ho reálně stihlo.
 */
@Entity
@Table(name = "amrap_round_entry")
@Getter
@Setter
@NoArgsConstructor
public class AmrapRoundEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amrap_config_id", nullable = false)
    private AmrapConfigEntity amrapConfig;

    /** 1-based pořadí kola. */
    @Column(name = "round_index", nullable = false)
    private Integer roundIndex;

    /** Odpovídá {@code AmrapStepEntity.orderIndex} (0-based). */
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private boolean skipped = false;

    @Column(name = "actual_reps")
    private Integer actualReps;

    @Column(name = "actual_weight_kg", precision = 7, scale = 2)
    private BigDecimal actualWeightKg;

    @Column(length = 255)
    private String note;
}
