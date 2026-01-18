package com.ragnarok.ragnarok_customers_training_diary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "training")
@Table(name = "training")
@Getter
@Setter
public class TrainingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trainingId;

    //provázat s konkrétním userem, který trénink ukládá
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<ExerciseEntity> exercises;

    //...?


    //type of training
    private boolean kb;
    private boolean strengthAndCardio;
    private boolean bodyweight;
    private boolean cardio;
    private boolean OsResets;
    private boolean anotherTrainingType;

    //RPE
    private int rpe;

    // Inner logic is necessary for evaluate and fill up variables in this part.
    //trainingDifficulty
    private boolean easy;
    private boolean medium;
    private boolean hard;
    private boolean anotherTrainingDifficulty;

//    private String nameOfLesson;
//
//    private int numberOfFreeSlots;
//
//    private String coachName;
//
//    private LocalDateTime dateOfCurrentLesson;
//
//    private LocalDateTime startOfCurrentLesson;
//
//    private LocalDateTime endOfCurrentLesson;
//
//    private int repeatIntervalInDays;
//
//    private int numberOfCopyConcreteTraining;
//
//    private Long parentTrainingId;

//    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
//    private List<ReservationEntity> reservations;
}
