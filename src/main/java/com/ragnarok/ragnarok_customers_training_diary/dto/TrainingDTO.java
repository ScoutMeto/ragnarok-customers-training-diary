package com.ragnarok.ragnarok_customers_training_diary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    private List<ExerciseDTO> exercises;
    private String nameOfLesson;

    private int numberOfFreeSlots;

    private String coachName;

    private LocalDateTime dateOfCurrentLesson;

    private LocalDateTime startOfCurrentLesson;

    private LocalDateTime endOfCurrentLesson;

    private int repeatIntervalInDays = 7;

    private int numberOfCopyConcreteTraining = 0;

    private Long parentTrainingId;

}