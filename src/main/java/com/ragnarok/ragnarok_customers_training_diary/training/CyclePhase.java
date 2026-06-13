package com.ragnarok.ragnarok_customers_training_diary.training;

/**
 * Fáze ženského menstruačního cyklu (ScoutMeto kolo 5). Záznam je nepovinný a dělá ho
 * jen uživatelka označená jako {@code FEMALE}. Písmeno se zobrazuje v cyklus-kalendáři,
 * celý název v tooltipu / pod dnem.
 */
public enum CyclePhase {
    MENSTRUAL("M", "Menstruační fáze", "obvykle 1.–5. den"),
    FOLLICULAR("F", "Folikulární fáze", "obvykle 6.–13. den"),
    OVULATORY("O", "Ovulační fáze", "obvykle 14. den"),
    LUTEAL("L", "Luteální fáze", "obvykle 15.–28. den");

    private final String letter;
    private final String label;
    private final String hint;

    CyclePhase(String letter, String label, String hint) {
        this.letter = letter;
        this.label = label;
        this.hint = hint;
    }

    public String getLetter() {
        return letter;
    }

    public String getLabel() {
        return label;
    }

    public String getHint() {
        return hint;
    }

    /** Text pro roletu ve formuláři, např. „Menstruační fáze (obvykle 1.–5. den)". */
    public String getSelectLabel() {
        return label + " (" + hint + ")";
    }
}
