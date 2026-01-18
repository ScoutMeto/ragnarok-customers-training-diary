package com.ragnarok.ragnarok_customers_training_diary.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExerciseDTO {


    //due to Duration need to have time in min. and sec. in DTO
    private Integer minutes;
    private Integer seconds;
}
