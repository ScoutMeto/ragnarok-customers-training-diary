package com.ragnarok.ragnarok_customers_training_diary.training;

/**
 * Viditelnost tréninku.
 *
 * <ul>
 *     <li><b>PRIVATE</b> — klientův osobní záznam, vidí jen majitel (a admin v admin sekci).
 *         Musí mít {@code owner}.</li>
 *     <li><b>GROUP</b> — skupinový trénink vytvořený adminem, viditelný všem klientům
 *         pro daný den. {@code owner} je NULL, {@code createdBy} = admin.</li>
 *     <li><b>TEMPLATE</b> — šablona tréninku v admin sekci. Trenér ji „přiřadí"
 *         konkrétnímu klientovi na konkrétní datum a vznikne PRIVATE instance
 *         s odkazem přes {@link TrainingEntity#sourceTemplate}. Sama šablona
 *         se nezobrazuje v deníku klienta.</li>
 * </ul>
 */
public enum TrainingVisibility {
    PRIVATE,
    GROUP,
    TEMPLATE
}
