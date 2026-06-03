package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phase 13 (A12): KB sport time. Čas zadán jako minuty + sekundy (převod na total_seconds). */
@Getter
@Setter
@NoArgsConstructor
public class KbSportConfigInput {

    @Min(0) @Max(600)
    private Integer totalMinutes;

    @Min(0) @Max(59)
    private Integer totalSeconds;

    private Integer totalReps;

    /** Délka intervalu pro podrobný záznam (s). null/0 = jen souhrn. */
    private Integer splitIntervalSeconds;

    private BigDecimal weightKg;

    private boolean unilateral;

    private String notes;

    private List<IntervalInput> intervals = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class IntervalInput {
        private Integer intervalIndex;
        private Integer reps;
        /** "L" / "P" / null. */
        private String side;
        private String note;
    }
}
