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
public class AmrapConfigInput {

    @Min(30) @Max(7200)
    private Integer timecapSeconds;

    private Integer targetRepsPerRound;

    private BigDecimal targetWeightKg;

    private Integer roundsCompleted;

    private Integer extraReps;

    private String notes;
}
