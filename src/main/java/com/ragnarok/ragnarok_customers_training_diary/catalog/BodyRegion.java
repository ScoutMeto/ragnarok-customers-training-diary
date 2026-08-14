package com.ragnarok.ragnarok_customers_training_diary.catalog;

/**
 * Hrubá oblast těla pro filtrování katalogu cviků a statistiky volume per body part.
 *
 * <p>kolo 10: popisky česky a shodné s názvy systémových tagů (Celé tělo / Horní část těla /
 * Dolní část těla / Střed těla), aby se katalog a tagy daly ve statistikách míchat.
 */
public enum BodyRegion {
    FULL_BODY("Celé tělo"),
    UPPER_BODY("Horní část těla"),
    LOWER_BODY("Dolní část těla"),
    CORE("Střed těla");

    private final String label;

    BodyRegion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Popisek pro uloženou hodnotu; neznámou hodnotu vrátí beze změny. */
    public static String labelOf(String name) {
        if (name == null) return null;
        for (BodyRegion r : values()) {
            if (r.name().equals(name)) return r.label;
        }
        return name;
    }
}
