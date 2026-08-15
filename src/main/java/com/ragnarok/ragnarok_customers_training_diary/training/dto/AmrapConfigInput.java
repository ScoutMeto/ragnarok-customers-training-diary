package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AMRAP (kolo 10): sada cviků, která se opakuje, dokud nedojde čas.
 * Délka se zadává v minutách + sekundách, kanonicky se ukládá v sekundách.
 */
@Getter
@Setter
@NoArgsConstructor
public class AmrapConfigInput {

    @Min(1) @Max(14400)
    private Integer timecapSeconds;

    /** „Délka AMRAP" ve formuláři — minuty + sekundy. */
    private Integer timecapMin;
    private Integer timecapSec;

    private Integer targetRepsPerRound;

    private BigDecimal targetWeightKg;

    private Integer roundsCompleted;

    private Integer extraReps;

    private String notes;

    /** Cviky, které se v kole opakují. */
    private List<StepInput> steps = new ArrayList<>();

    /** Skutečný záznam po kolech (poslední kolo bývá rozjeté). */
    private List<RoundEntryInput> roundEntries = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StepInput {
        private Integer orderIndex;
        private String name;
        private Integer reps;
        @jakarta.validation.constraints.Pattern(regexp = "REPS|METERS|SECONDS")
        private String repUnit;
        private BigDecimal weightKg;
        private String note;
        /** Výška překážky nebo výskoku v centimetrech pro tag Skoky, výskoky. */
        private BigDecimal jumpHeightCm;
        private String equipmentName;
        private BigDecimal equipmentWeightKg;
        private Integer equipmentCount;
        private BigDecimal equipmentSecondWeightKg;
        private Set<Long> tagIds = new HashSet<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoundEntryInput {
        private Integer roundIndex;
        private Integer stepOrder;
        /** Cvik, na který v rozjetém kole nezbyl čas. */
        private boolean skipped;
        private Integer actualReps;
        private BigDecimal actualWeightKg;
        private String note;
        /** Výška překážky nebo výskoku v centimetrech pro tag Skoky, výskoky. */
        private BigDecimal jumpHeightCm;
    }
}
