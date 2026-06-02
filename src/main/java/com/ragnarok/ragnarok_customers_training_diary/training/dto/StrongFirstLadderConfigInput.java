package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phase 13 (A10): StrongFirst žebřík. */
@Getter
@Setter
@NoArgsConstructor
public class StrongFirstLadderConfigInput {

    @Min(1) @Max(50)
    private Integer ladderHeight;

    @Min(1) @Max(50)
    private Integer cycles;

    private Integer restSeconds;

    private BigDecimal weightKg;

    private boolean unilateral;

    private String notes;
}
