package com.ragnarok.ragnarok_customers_training_diary.account;

/**
 * Pohlaví uživatele (ScoutMeto kolo 5). Nepovinné — {@code null} znamená neuvedeno.
 * U žen aplikace nabízí navíc záznam menstruačního cyklu a cyklus-kalendář ve statistikách.
 */
public enum Gender {
    MALE("Muž"),
    FEMALE("Žena");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
