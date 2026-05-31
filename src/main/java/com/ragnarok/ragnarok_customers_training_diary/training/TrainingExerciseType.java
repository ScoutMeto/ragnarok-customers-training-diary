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

    /** Kruhový trénink s opakovanými koly (Fáze 3). */
    CIRCUIT,

    TABATA,
    AMRAP,
    LADDER,
    STEPLADDER,
    PYRAMID,
    SUPERSET,
    STRAIGHT_SETS,
    COMPLEX
}
