package com.ragnarok.ragnarok_customers_training_diary.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmomMinuteOverrideDTO {

    private Long emomMinuteOverrideId;

    @Min(1)
    private int minuteIndex;

    @Min(0)
    private Integer reps;

    @Min(0)
    private Double weight;

    private String notes;
}