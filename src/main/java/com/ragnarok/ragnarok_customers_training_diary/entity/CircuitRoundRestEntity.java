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

@Entity(name = "circuit_round_rest")
@Table(name = "circuit_round_rest")
@Getter
@Setter
public class CircuitRoundRestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long circuitRoundRestId;

    @ManyToOne
    @JoinColumn(name = "circuit_id", referencedColumnName = "circuitId", nullable = false)
    private CircuitEntity circuit;

    private int roundIndex;
    private Integer restSeconds;
}