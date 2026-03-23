package com.ragnarok.ragnarok_customers_training_diary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "exercise")
@Table(name = "exercise")
public class ExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exerciseId;

    @ManyToOne
    @JoinColumn(name = "training_id", referencedColumnName = "trainingId", nullable = false)
    private TrainingEntity training;

    private String nameOfExercise;
    private String note;
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    private ExerciseType exerciseType;

    @OneToOne(mappedBy = "exercise", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseDTO customizingExercise;

    @OneToOne(mappedBy = "exercise", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private EmomEntity emom;

    @OneToOne(mappedBy = "exercise", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private CircuitEntity circuit;

}
    //pyramid!

    //...

    //... next?

/*
pro časy intervalů a pauz použiju Duration class v Javě
 */

//    Rules:
//    -client could use more than one feature of exercise -> for example "complex" + "interval" or "emom"
//    -all workout is creating in real time by the form and client could see, edit and save at the end of process -> overview list of workout
//    -everything in overview could be edited
//    -first of all -> which type of exercise client will create? (simple/basic type; row; circuit; ladder; stepladder; pyramid; .... -> specific form will be used for each one
//    -


//  MAYBE HERE WILL BE ONLY ALL THE LISTS OF CONCRETE TYPE (simple/basic type; row; circuit; ladder; stepladder; pyramid; ....)
//  AND each type will have OWN DTO AND ENTITY
//  ...mind another workouts, like barbell WO, bodyweight WO...


//___//
//    //ladder
//    private boolean ladderYesOrNo;
//    private int ladderSteps; //System automatically calculate number of sets and reps and write to overview
//
//    //stepladder - Nikolin žebřík
//    private boolean stepladderYesOrNo;
//    private int stepLadderSteps; //System automatically calculate number of sets and reps and write to overview
////___//
//
////___//
//    //rows
//    private boolean rowYesOrNo;
//    double[] usedWeights; //Frontend mechanics for add all used weights and calculate and write to overview
////___//
//
////___//
//    //circuit
//    private boolean circuitYesOrNo;
//    private int repetition; //shutter with numbers -> if client chose something, system repeat all workout at main overview of workout
////  if yes -> at frontend show specific form.
////  System provide special ID (circuitId) of all checklist of exercises and each exercise will share this one (in database).
////  if yes -> if client want, at frontend client could describe whole circuit.
//    //circuitId
//    private long circuitId; //description above
//    //circuitDescription
//    private String circuitDescription; //description above
////___//
//
//
//    @OneToOne(mappedBy = "exercise", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
//    private CircuitEntity circuit;
