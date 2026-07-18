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

@Entity
@Table(name = "circuit_step")
@Getter
@Setter
@NoArgsConstructor
public class CircuitStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circuit_config_id", nullable = false)
    private CircuitConfigEntity circuitConfig;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(nullable = false, length = 128)
    private String name;

    private Integer reps;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    /** ScoutMeto kolo 9: jednotka pole „Opakování" — NULL=opakování, METERS/SECONDS (Carry/Isometrie). */
    @Column(name = "rep_unit", length = 10)
    private String repUnit;

    @Column(length = 255)
    private String note;

    // ScoutMeto kolo 8: náčiní se zadává u každého cviku kruhového tréninku zvlášť
    @Column(name = "equipment_name", length = 64)
    private String equipmentName;

    @Column(name = "equipment_weight_kg", precision = 7, scale = 2)
    private BigDecimal equipmentWeightKg;

    @Column(name = "equipment_count", nullable = false)
    private int equipmentCount = 1;

    @Column(name = "equipment_second_weight_kg", precision = 7, scale = 2)
    private BigDecimal equipmentSecondWeightKg;

    /** ScoutMeto kolo 8: zaměření (tagy) per cvik kruhového tréninku. */
    @jakarta.persistence.ManyToMany(fetch = FetchType.LAZY)
    @jakarta.persistence.OrderBy("name ASC")
    @jakarta.persistence.JoinTable(name = "circuit_step_tag_link",
            joinColumns = @JoinColumn(name = "circuit_step_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private java.util.Set<com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity> tags
            = new java.util.HashSet<>();
}
