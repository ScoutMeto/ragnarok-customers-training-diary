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

    /** kolo 10: CIRCUIT (výchozí) / SUPERSET / COMPLEX. */
    @jakarta.validation.constraints.Pattern(regexp = "CIRCUIT|SUPERSET|COMPLEX")
    private String mode = "CIRCUIT";

    @Min(1) @Max(30)
    private Integer rounds = 3;

    /**
     * Kanonická hodnota v sekundách. kolo 10: uživatel ji zadává ve dvou polích
     * (minuty + sekundy), mapper si z nich součet spočítá.
     */
    private Integer restBetweenRoundsS;

    private Integer restBetweenRoundsMin;
    private Integer restBetweenRoundsSec;

    /** Sekundy z dvojice minuty+sekundy; {@code null}, když uživatel nevyplnil ani jedno. */
    public static Integer toSeconds(Integer min, Integer sec, Integer fallbackSeconds) {
        if (min == null && sec == null) return fallbackSeconds;
        return (min != null ? min * 60 : 0) + (sec != null ? sec : 0);
    }

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
        /** ScoutMeto kolo 9: jednotka Opakování — null/REPS, METERS/SECONDS (Carry/Isometrie). */
        @jakarta.validation.constraints.Pattern(regexp = "REPS|METERS|SECONDS")
        private String repUnit;
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
        // kolo 10: pauza na konci konkrétního kola se zadává v minutách + sekundách
        private Integer restMin;
        private Integer restSec;
    }
}
