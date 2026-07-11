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
public class CircuitConfigInput {

    @Min(1) @Max(30)
    private Integer rounds = 3;

    private Integer restBetweenRoundsS;

    private String notes;

    private List<StepInput> steps = new ArrayList<>();

    private List<RoundRestInput> roundRests = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StepInput {
        private Integer orderIndex;
        private String name;
        private Integer reps;
        private Integer durationSeconds;
        private BigDecimal weightKg;
        private Integer restSeconds;
        private String note;
        // ScoutMeto kolo 8: náčiní + tagy per cvik kruhového tréninku
        private String equipmentName;
        private BigDecimal equipmentWeightKg;
        private Integer equipmentCount;
        private BigDecimal equipmentSecondWeightKg;
        private java.util.Set<Long> tagIds = new java.util.HashSet<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoundRestInput {
        private Integer roundIndex;
        private Integer restSeconds;
    }
}
