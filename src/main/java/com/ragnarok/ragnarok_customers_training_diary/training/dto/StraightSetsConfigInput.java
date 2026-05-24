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

    private Integer restSeconds;

    private String notes;
}
