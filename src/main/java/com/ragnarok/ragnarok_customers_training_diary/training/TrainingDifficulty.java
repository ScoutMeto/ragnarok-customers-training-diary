package com.ragnarok.ragnarok_customers_training_diary.training;

/**
 * Subjektivní obtížnost tréninku — klient si nastavuje sám po dokončení.
 * Nullable v DB — pokud klient neřeknul, statistiky to ignorují.
 */
public enum TrainingDifficulty {
    LIGHT,
    MEDIUM,
    HARD
}
