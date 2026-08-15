package com.ragnarok.ragnarok_customers_training_diary.tag;

public enum TagCategory {
    GENERAL("Obecné"),
    BODY_REGION("Oblast těla"),
    MOVEMENT_PATTERN("Pohybový vzorec"),
    EQUIPMENT("Náčiní/nářadí/pomůcky");

    private final String label;

    TagCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}