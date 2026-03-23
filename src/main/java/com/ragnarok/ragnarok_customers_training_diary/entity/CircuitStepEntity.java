package com.ragnarok.ragnarok_customers_training_diary.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "circuit_step")
@Table(name = "circuit_step")
@Getter
@Setter
public class CircuitStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long circuitStepId;

    @ManyToOne
    @JoinColumn(name = "circuit_id", referencedColumnName = "circuitId", nullable = false)
    private CircuitEntity circuit;

    private int orderIndex;
    private String name;
    private Integer reps;
    private Integer timeSeconds;
    private Double weight;
    private Integer restSeconds;
    private String notes;
}