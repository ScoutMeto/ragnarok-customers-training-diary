package com.ragnarok.ragnarok_customers_training_diary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmomDTO {

    private Long emomId;

    @NotNull
    private Long exerciseId;

    private String notes;

    @Min(1)
    private int totalMinutes;

    @Min(1)
    private int intervalSeconds = 60;

    @Min(0)
    private Integer defaultReps;

    @Min(0)
    private Double defaultWeight;

    @Valid
    private List<EmomMinuteOverrideDTO> minuteOverrides = new ArrayList<>();
}