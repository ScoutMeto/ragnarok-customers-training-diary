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
public class TabataConfigInput {

    @Min(1) @Max(30)
    private Integer rounds = 8;

    @Min(5) @Max(300)
    private Integer workSeconds = 20;

    @Min(0) @Max(300)
    private Integer restSeconds = 10;

    private Integer defaultReps;

    private BigDecimal defaultWeightKg;

    private String notes;

    private List<RoundOverrideInput> roundOverrides = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoundOverrideInput {
        private Integer roundIndex;
        private Integer reps;
        private BigDecimal weightKg;
        private String note;
    }
}
