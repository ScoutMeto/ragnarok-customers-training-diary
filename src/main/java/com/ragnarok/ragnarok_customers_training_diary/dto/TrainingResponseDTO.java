package com.ragnarok.ragnarok_customers_training_diary.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TrainingResponseDTO {
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private int numberOfTotalFreeSlots;
    private Long trainingId;

    private Map<String, Object> extendedProps;

}