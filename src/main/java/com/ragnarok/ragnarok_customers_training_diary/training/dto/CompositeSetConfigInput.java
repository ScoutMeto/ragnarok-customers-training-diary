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
public class CompositeSetConfigInput {

    @Min(1) @Max(30)
    private Integer rounds = 3;

    private BigDecimal sharedWeightKg;

    private Integer restBetweenRoundsS;

    private String notes;

    private List<StepInput> steps = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StepInput {
        private Integer orderIndex;
        private String name;
        private Integer reps;
        private BigDecimal weightKg;
        private Integer restAfterSeconds;
        private String note;
    }
}
