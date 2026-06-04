package com.ragnarok.ragnarok_customers_training_diary.training;

/**
 * Zaměření / obtížnost tréninku — klient si vybírá po dokončení. Nullable v DB.
 *
 * <p>Phase 9 (A13): místo původních LIGHT/MEDIUM/HARD má klient na výběr 9 konkrétních
 * zaměření rozdělených do 3 úrovní. V UI roletě se zobrazují jen české labely
 * (NE slova light/medium/hard — matoucí pro uživatele). Statistiky agregují podle
 * {@link Level} (= pořád 3 sloupce).
 */
public enum TrainingDifficulty {

    // ----- Úroveň 1 (lehká) -----
    LEHKY_TRENINK(Level.LIGHT, "Lehký trénink"),
    DELOAD(Level.LIGHT, "Deload"),
    RYCHLOST(Level.LIGHT, "Rychlost"),

    // ----- Úroveň 2 (střední) -----
    SILOVE_KONDICNI(Level.MEDIUM, "Silově-kondiční trénink"),
    SBER_OPAKOVANI(Level.MEDIUM, "Sběr opakování"),
    DRILL(Level.MEDIUM, "Drill"),
    VYUKA(Level.MEDIUM, "Výuka"),

    // ----- Úroveň 3 (těžká) -----
    ROZVOJ_MAX_SILY(Level.HARD, "Rozvoj maximální síly"),
    TESTOVANI(Level.HARD, "Testovací trénink");

    private final Level level;
    private final String label;

    TrainingDifficulty(Level level, String label) {
        this.level = level;
        this.label = label;
    }

    public Level getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Phase 19a (ScoutMeto): roleta obtížnosti se zobrazuje jako 3 řádky (jeden na úroveň),
     * text = spojené labely dané úrovně. Hodnota = reprezentativní {@link TrainingDifficulty}
     * (první v úrovni) — v DB i statistikách zůstává 3-úrovňové dělení beze změny.
     */
    public record Group(TrainingDifficulty representative, String label) {}

    public static java.util.List<Group> groups() {
        java.util.List<Group> result = new java.util.ArrayList<>();
        for (Level lvl : Level.values()) {
            TrainingDifficulty rep = null;
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (TrainingDifficulty d : values()) {
                if (d.level == lvl) {
                    if (rep == null) rep = d;
                    labels.add(d.label);
                }
            }
            if (rep != null) {
                result.add(new Group(rep, String.join(", ", labels)));
            }
        }
        return result;
    }

    /**
     * Tři úrovně náročnosti pro agregaci ve statistikách. Label se používá
     * jako popisek sloupce v grafu „objem podle obtížnosti".
     */
    public enum Level {
        LIGHT("Lehký / regenerace"),
        MEDIUM("Silově-kondiční"),
        HARD("Maximální síla / test");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
