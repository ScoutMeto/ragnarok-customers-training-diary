package com.ragnarok.ragnarok_customers_training_diary.training.types.amrap;

import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cvik v sadě AMRAPu (kolo 10). Struktura odpovídá kroku kruhového tréninku —
 * AMRAP je kruhový trénink bez předdefinovaných pauz, s časovým limitem.
 */
@Entity
@Table(name = "amrap_step")
@Getter
@Setter
@NoArgsConstructor
public class AmrapStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amrap_config_id", nullable = false)
    private AmrapConfigEntity amrapConfig;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(nullable = false, length = 128)
    private String name;

    /** Cíl na kolo — opakování, nebo metry/sekundy podle {@link #repUnit}. */
    private Integer reps;

    /** NULL = opakování; METERS/SECONDS pro Nošení a Izometrii. */
    @Column(name = "rep_unit", length = 10)
    private String repUnit;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(length = 255)
    private String note;

    @Column(name = "jump_height_cm", precision = 6, scale = 2)
    private BigDecimal jumpHeightCm;

    @Column(name = "equipment_name", length = 64)
    private String equipmentName;

    @Column(name = "equipment_weight_kg", precision = 7, scale = 2)
    private BigDecimal equipmentWeightKg;

    @Column(name = "equipment_count", nullable = false)
    private int equipmentCount = 1;

    @Column(name = "equipment_second_weight_kg", precision = 7, scale = 2)
    private BigDecimal equipmentSecondWeightKg;

    @ManyToMany(fetch = FetchType.LAZY)
    @OrderBy("name ASC")
    @JoinTable(name = "amrap_step_tag_link",
            joinColumns = @JoinColumn(name = "amrap_step_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TrainingTagEntity> tags = new HashSet<>();
}
