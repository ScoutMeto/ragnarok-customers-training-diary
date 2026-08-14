package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dlouhé pomalé kardio (kolo 10). Časy se zadávají po složkách (h:m:s), kanonicky
 * se ukládají v sekundách.
 */
@Getter
@Setter
@NoArgsConstructor
public class CardioConfigInput {

    // Celkový čas (včetně přestávek) — jediný povinný údaj CARDIA.
    private Integer elapsedHours;
    private Integer elapsedMin;
    private Integer elapsedSec;

    // Čistý čas — nepovinný, dopočítá se z celkového času a pauz.
    private Integer activeHours;
    private Integer activeMin;
    private Integer activeSec;

    /** Vzdálenost tak, jak ji uživatel zadal — jednotku určuje {@link #distanceUnit}. */
    private BigDecimal distance;

    /** M nebo KM; interně se vždy ukládá v metrech. */
    private String distanceUnit;

    private Integer repetitions;
    private Integer steps;
    private Integer elevationGainM;

    /** Ruční přepis odvozených hodnot (např. přesnější číslo z hodinek). */
    private BigDecimal avgSpeedKmh;
    private Integer avgPaceMin;
    private Integer avgPaceSec;

    private String notes;

    private List<PauseInput> pauses = new ArrayList<>();

    /** Sekundy z h:m:s; {@code null}, když uživatel nevyplnil nic. */
    public static Integer toSeconds(Integer hours, Integer min, Integer sec) {
        if (hours == null && min == null && sec == null) return null;
        return (hours != null ? hours * 3600 : 0)
                + (min != null ? min * 60 : 0)
                + (sec != null ? sec : 0);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PauseInput {
        /** Čas od začátku aktivity, kdy pauza začala. */
        private Integer startHours;
        private Integer startMin;
        private Integer startSec;

        private Integer durationMin;
        private Integer durationSec;

        private boolean activePause;

        private BigDecimal distanceAtPause;
        private Integer repetitionsAtPause;
        private Integer stepsAtPause;
        private String note;
    }
}
