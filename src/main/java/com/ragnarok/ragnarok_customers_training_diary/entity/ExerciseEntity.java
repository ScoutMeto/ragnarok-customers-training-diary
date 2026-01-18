package com.ragnarok.ragnarok_customers_training_diary.entity;

import com.ragnarok.ragnarok_customers_training_diary.configuration.DurationToLongConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    //Time convertion (example 5:30 "min:sec")
    @Convert(converter = DurationToLongConverter.class)
    @Column(name = "duration_seconds", nullable = false)
    private Duration duration;

    //IDs of other variables

    //...?

    private String nameOfExercise;
    private double weightOrResistance; //facultative
    private String descriptionOfResistance; //facultative (if client could not able say weight or exact resistance -> example: "red expander"/"water during swimming"/...
    private int reps;
    private int sets;
    private int rest; //facultative
    private String note;

    //laterality
    private String leftSide;
    private String rightSide;
    private String bilateral;

    //body part
    private boolean fullBody;
    private boolean upperBody;
    private boolean lowerBody;
    private boolean specificPart;

    //movementPattern
    private boolean push;
    private boolean pull;
    private boolean rotation;
    private boolean lunge;
    private boolean isometric;
    private boolean carry;
    private boolean gait; //run or walk
    private boolean plyometrics;
    private boolean crawling;
    private boolean jumping;
    private boolean coreAndAbs;
    private boolean anotherMovementPattern; //write own describtion
//    -> at frontend show all of "movement patterns" and client should mark by checkbox each one.
//    In database has appropriate exercise one, or more than one feature.

    //Simple exercise type. Basic, with no specifics. For example -> military press, one weight, tha same rest, some sets and reps...
    private boolean simpleYesOrNo;


    //___//
    //complex
    private boolean complexYesOrNo;
//  if yes -> at frontend show specific form.
        //    System provide special ID (complexId) of all checklist of exercises and each exercise will share this one (in database).
//   if yes -> if client want, at frontend client could describe whole complex.

    //complexId
    private long complexId; //description above
    //complexDescription
    private String complexDescription; //description above
//___//

    //intervals -
    private boolean intervalsYesOrNo;
    private int intervalInMinutes;
    private boolean repsOrFeeling;
    private int ifRepsHowMuch;
    private int ifFeelingHowIntensity;
    private int restInSeconds; //facultative

    //EMOM
    private boolean emomYesOrNo;
    private int emomIntervalInMinutes;
    private int emomRepsInMinute;

//___//
    //ladder
    private boolean ladderYesOrNo;
    private int ladderSteps; //System automatically calculate number of sets and reps and write to overview

    //stepladder - Nikolin žebřík
    private boolean stepladderYesOrNo;
    private int stepLadderSteps; //System automatically calculate number of sets and reps and write to overview
//___//

//___//
    //rows
    private boolean rowYesOrNo;
    double[] usedWeights; //Frontend mechanics for add all used weights and calculate and write to overview
//___//

//___//
    //circuit
    private boolean circuitYesOrNo;
    private int repetition; //shutter with numbers -> if client chose something, system repeat all workout at main overview of workout
//  if yes -> at frontend show specific form.
//  System provide special ID (circuitId) of all checklist of exercises and each exercise will share this one (in database).
//  if yes -> if client want, at frontend client could describe whole circuit.
    //circuitId
    private long circuitId; //description above
    //circuitDescription
    private String circuitDescription; //description above
//___//

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
}

//  MAYBE HERE WILL BE ONLY ALL THE LISTS OF CONCRETE TYPE (simple/basic type; row; circuit; ladder; stepladder; pyramid; ....)
//  AND each type will have OWN DTO AND ENTITY
//  ...mind another workouts, like barbell WO, bodyweight WO...
