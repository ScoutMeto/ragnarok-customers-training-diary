package com.ragnarok.ragnarok_customers_training_diary.dto;

import com.ragnarok.ragnarok_customers_training_diary.configuration.DurationToLongConverter;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "customizing_exercise")
@Table(name = "customizing_exercise")
public class CustomizingExerciseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customizingExerciseId;

    private Long exerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", referencedColumnName = "exerciseId", nullable = false, unique = true)
    private ExerciseEntity exercise;

    @Convert(converter = DurationToLongConverter.class)
    @Column(name = "duration_seconds", nullable = false)
    private Duration duration;

    // due to Duration need to have time in min. and sec. in DTO
    private Integer minutes;
    private Integer seconds;

    private String descriptionOfResistance;
    private Double weightOrResistance;
    private Integer reps;
    private Integer sets;
    private Integer rest;
    private String leftSide;
    private String rightSide;
    private String bilateral;

    private boolean fullBody;
    private boolean upperBody;
    private boolean lowerBody;
    private boolean specificPart;

    private boolean push;
    private boolean pull;
    private boolean rotation;
    private boolean lunge;
    private boolean isometric;
    private boolean carry;
    private boolean gait;
    private boolean plyometrics;
    private boolean crawling;
    private boolean jumping;
    private boolean coreAndAbs;
    private boolean anotherMovementPattern;
}