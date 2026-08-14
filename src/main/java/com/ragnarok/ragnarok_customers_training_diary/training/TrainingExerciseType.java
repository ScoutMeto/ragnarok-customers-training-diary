package com.ragnarok.ragnarok_customers_training_diary.training;

/**
 * Typ provedení cviku v tréninku. Fáze 1 implementuje pouze {@link #FREEFORM} (jednoduchý
 * formulář s počty setů a opakování). Specifické typy (EMOM, CIRCUIT atd.) přijdou ve Fázi 3.
 *
 * <p>Položky jsou v jednom enumu i v DB jako VARCHAR — připravené pro budoucí typy bez
 * další migrace.
 */
public enum TrainingExerciseType {
    /** Jednoduchý cvik: název + N setů (váha, opakování, RPE, poznámka). */
    FREEFORM,

    /** Every Minute On the Minute (Fáze 3). */
    EMOM,

    /**
     * Kruhový trénink s opakovanými koly (Fáze 3). Od kola 10 zastřešuje i SUPERSET
     * a COMPLEX — liší se jen režimem ({@code circuit_config.mode}), ne strukturou.
     */
    CIRCUIT,

    TABATA,
    AMRAP,
    LADDER,
    STEPLADDER,
    PYRAMID,
    STRAIGHT_SETS,

    /** Intervalový trénink — Phase 13 (A11): N kol práce (reps/čas) + pauza. */
    INTERVAL,

    /** StrongFirst žebřík — Phase 13 (A10): 1,2,...,ladderHeight reps + pauza, opakuj v cyklech. */
    STRONGFIRST_LADDER,

    /** KB sport time — Phase 13 (A12): počet opakování za čas + volitelný split na intervaly + L/P. */
    KB_SPORT_TIME,

    /** Kardio cvik (běh, veslo, kolo…) — Phase 11 (A1). Zatím bez speciálního configu. */
    CARDIO

    // CORE zrušen (ScoutMeto kolo 8) — V40 převedla existující záznamy na FREEFORM
    // SUPERSET a COMPLEX zrušeny (kolo 10) — V44 je převedla na CIRCUIT s režimem
}
