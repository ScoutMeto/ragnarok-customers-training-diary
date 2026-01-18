package com.ragnarok.ragnarok_customers_training_diary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TrainingDTO {

    @JsonProperty("training_id")
    private Long trainingId;

    //provázat s konkrétním userem, který trénink ukládá
    private Long userId;

    private List<ExerciseEntity> exercises;



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
//    private int repeatIntervalInDays = 7;
//
//    private int numberOfCopyConcreteTraining = 0;
//
//    private Long parentTrainingId;

}
