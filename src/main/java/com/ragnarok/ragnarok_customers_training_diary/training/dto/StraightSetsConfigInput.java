package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StraightSetsConfigInput {

    @Min(1) @Max(50)
    private Integer setCount;

    private Integer repsPerSet;

    private BigDecimal weightKg;

    /** Kanonicky v sekundách; formulář zadává minuty + sekundy. */
    private Integer restSeconds;

    private Integer restMin;
    private Integer restSec;

    private String notes;

    /** kolo 10: skutečně odcvičené sety. */
    private java.util.List<RowInput> rows = new java.util.ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RowInput {
        private Integer reps;
        private BigDecimal weightKg;
        private Integer restSeconds;
    }
}
