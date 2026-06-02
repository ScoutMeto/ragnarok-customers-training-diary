package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phase 13 (A11): Interval — N kol, práce (reps NEBO čas) + pauza. */
@Getter
@Setter
@NoArgsConstructor
public class IntervalConfigInput {

    @Min(1) @Max(100)
    private Integer rounds;

    private Integer workReps;

    private Integer workSeconds;

    private Integer restSeconds;

    private BigDecimal weightKg;

    private String notes;
}
