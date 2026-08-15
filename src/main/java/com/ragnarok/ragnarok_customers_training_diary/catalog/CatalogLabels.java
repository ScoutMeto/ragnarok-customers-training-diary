package com.ragnarok.ragnarok_customers_training_diary.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

/** Display labels for stable catalog keys kept in the database. */
public final class CatalogLabels {

    private static final Map<String, String> BODY_REGION_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> MOVEMENT_PATTERN_LABELS = new LinkedHashMap<>();

    static {
        BODY_REGION_LABELS.put("FULL_BODY", "Celé tělo");
        BODY_REGION_LABELS.put("UPPER_BODY", "Horní část těla");
        BODY_REGION_LABELS.put("LOWER_BODY", "Dolní část těla");
        BODY_REGION_LABELS.put("CORE", "Střed těla");

        MOVEMENT_PATTERN_LABELS.put("CARRY", "Nošení");
        MOVEMENT_PATTERN_LABELS.put("GAIT", "Běh");
        MOVEMENT_PATTERN_LABELS.put("HINGE", "Kyčelní ohyb");
        MOVEMENT_PATTERN_LABELS.put("ISOMETRY", "Isometrie");
        MOVEMENT_PATTERN_LABELS.put("ISOMETRIC", "Isometrie");
        MOVEMENT_PATTERN_LABELS.put("LUNGE", "Výpad");
        MOVEMENT_PATTERN_LABELS.put("OTHER", "Jiné");
        MOVEMENT_PATTERN_LABELS.put("OTHERS", "Jiné");
        MOVEMENT_PATTERN_LABELS.put("PLYO", "Plyometrie");
        MOVEMENT_PATTERN_LABELS.put("PULL", "Tah");
        MOVEMENT_PATTERN_LABELS.put("PUSH", "Tlak");
        MOVEMENT_PATTERN_LABELS.put("ROTATION", "Rotace");
        MOVEMENT_PATTERN_LABELS.put("SQUAT", "Dřep");
        MOVEMENT_PATTERN_LABELS.put("LOCOMOTION", "Lokomoce a animal movements");
        MOVEMENT_PATTERN_LABELS.put("JUMPS", "Skoky, výskoky");
        MOVEMENT_PATTERN_LABELS.put("COORDINATION", "Koordinace");
    }

    private CatalogLabels() {
    }

    public static String bodyRegion(String value) {
        return label(BODY_REGION_LABELS, value);
    }

    public static String movementPattern(String value) {
        return label(MOVEMENT_PATTERN_LABELS, value);
    }

    private static String label(Map<String, String> labels, String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return labels.getOrDefault(value.trim().toUpperCase(), value);
    }

    public static Map<String, String> bodyRegionLabels() {
        return BODY_REGION_LABELS;
    }

    public static Map<String, String> movementPatternLabels() {
        return MOVEMENT_PATTERN_LABELS;
    }

    public static String origin(ExerciseCatalogItemEntity item) {
        return item.isSystem() ? "SYSTÉM" : "MŮJ";
    }
}