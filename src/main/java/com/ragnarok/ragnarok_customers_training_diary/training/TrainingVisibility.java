package com.ragnarok.ragnarok_customers_training_diary.training;

/**
 * Viditelnost tréninku.
 *
 * <ul>
 *     <li><b>PRIVATE</b> — klientův osobní záznam, vidí jen majitel (a admin v admin sekci).
 *         Musí mít {@code owner}.</li>
 *     <li><b>GROUP</b> — skupinový trénink vytvořený adminem, viditelný všem klientům
 *         pro daný den. {@code owner} je NULL, {@code createdBy} = admin.</li>
 * </ul>
 */
public enum TrainingVisibility {
    PRIVATE,
    GROUP
}
