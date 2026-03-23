package com.ragnarok.ragnarok_customers_training_diary.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "circuit")
@Table(name = "circuit")
@Getter
@Setter
public class CircuitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long circuitId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", referencedColumnName = "exerciseId", nullable = false, unique = true)
    private ExerciseEntity exercise;

    private String notes;
    private int rounds;

    @OneToMany(mappedBy = "circuit", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<CircuitStepEntity> steps = new ArrayList<>();

    @OneToMany(mappedBy = "circuit", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<CircuitRoundRestEntity> roundRests = new ArrayList<>();
}