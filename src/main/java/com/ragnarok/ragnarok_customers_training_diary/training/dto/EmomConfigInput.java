package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmomConfigInput {

    @Min(1) @Max(120)
    private Integer totalMinutes;

    @Min(1) @Max(600)
    private Integer intervalSeconds = 60;

    private Integer defaultReps;

    private BigDecimal defaultWeightKg;

    private String notes;

    private List<MinuteOverrideInput> minuteOverrides = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MinuteOverrideInput {
        private Integer minuteIndex;
        private Integer reps;
        private BigDecimal weightKg;
        private String note;
    }
}
