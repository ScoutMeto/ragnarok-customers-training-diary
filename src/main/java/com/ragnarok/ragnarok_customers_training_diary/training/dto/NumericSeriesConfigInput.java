package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NumericSeriesConfigInput {

    private Integer startValue = 1;

    private Integer peakValue;

    private Integer stepSize = 1;

    @Size(max = 255)
    private String repSequenceCsv;

    private BigDecimal weightKg;

    private Integer restSecondsBetween;

    private String notes;

    /** ScoutMeto kolo 8: vygenerovaná/editovaná tabulka řádků série. */
    private java.util.List<RowInput> rows = new java.util.ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RowInput {
        private Integer rowIndex;
        /** Číslo série/stupně (label „N. série"). */
        private Integer rung;
        private Integer reps;
        private BigDecimal weightKg;
    }
}
