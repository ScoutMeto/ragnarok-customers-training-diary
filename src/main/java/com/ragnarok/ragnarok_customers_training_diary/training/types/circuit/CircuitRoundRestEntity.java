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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "circuit_round_rest")
@Getter
@Setter
@NoArgsConstructor
public class CircuitRoundRestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circuit_config_id", nullable = false)
    private CircuitConfigEntity circuitConfig;

    @Column(name = "round_index", nullable = false)
    private Integer roundIndex;

    @Column(name = "rest_seconds", nullable = false)
    private Integer restSeconds;
}
