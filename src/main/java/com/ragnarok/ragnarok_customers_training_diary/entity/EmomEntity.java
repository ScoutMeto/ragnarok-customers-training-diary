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

@Entity(name = "emom")
@Table(name = "emom")
@Getter
@Setter
public class EmomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emomId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", referencedColumnName = "exerciseId", nullable = false, unique = true)
    private ExerciseEntity exercise;

    private String notes;
    private int totalMinutes;
    private int intervalSeconds;
    private Integer defaultReps;
    private Double defaultWeight;

    @OneToMany(mappedBy = "emom", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<EmomMinuteOverrideEntity> minuteOverrides = new ArrayList<>();
}