package com.ragnarok.ragnarok_customers_training_diary.dto;

import com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExerciseDTO {

    private Long exerciseId;
    private Long trainingId;
    private String nameOfExercise;
    private String note;
    private Integer orderIndex;
    private ExerciseType exerciseType;

//    //due to Duration need to have time in min. and sec. in DTO
//    private Integer minutes;
//    private Integer seconds;
    private CustomizingExerciseDTO customizingExercise;
    private EmomDTO emom;
    private CircuitDTO circuit;
}