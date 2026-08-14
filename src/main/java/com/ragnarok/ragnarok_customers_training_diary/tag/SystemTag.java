package com.ragnarok.ragnarok_customers_training_diary.tag;

/**
 * Klíče systémových tagů (kolo 10). Odpovídají sloupci {@code training_tag.system_key}
 * seedovanému migrací V42.
 *
 * <p>Název tagu je jen popisek a smí se měnit (počeštění, úprava terminologie);
 * veškerá logika — jednotky ve formuláři i agregace ve statistikách — se řídí tímto klíčem.
 */
public final class SystemTag {

    private SystemTag() {}

    // --- charakter provedení (rozhoduje o jednotkách i o způsobu výpočtu statistik) ---

    /** Cvik se počítá na opakování (série / opakování / nazvedané kg). */
    public static final String REPETITIVE = "REPETITIVE";

    /** Nošení — místo opakování se zaznamenávají metry nebo sekundy. */
    public static final String CARRY = "CARRY";

    /** Izometrie — statická výdrž, jen sekundy (metry se nepřekonávají). */
    public static final String ISOMETRY = "ISOMETRY";

    // --- zaměření / oblast těla ---
    public static final String CORE = "CORE";
    public static final String FULL_BODY = "FULL_BODY";
    public static final String UPPER_BODY = "UPPER_BODY";
    public static final String LOWER_BODY = "LOWER_BODY";

    // --- lateralita ---
    public static final String UNILATERAL = "UNILATERAL";
    public static final String UNILATERAL_LEFT = "UNILATERAL_LEFT";
    public static final String UNILATERAL_RIGHT = "UNILATERAL_RIGHT";
    public static final String BILATERAL = "BILATERAL";

    // --- náčiní / charakter zátěže ---
    public static final String BODYWEIGHT = "BODYWEIGHT";
    public static final String KETTLEBELL = "KETTLEBELL";
    public static final String BARBELL = "BARBELL";

    // --- ostatní systémové ---
    public static final String CARDIO = "CARDIO";
    public static final String STRENGTH = "STRENGTH";
    public static final String STRENGTH_ENDURANCE = "STRENGTH_ENDURANCE";
    public static final String MOBILITY = "MOBILITY";
    public static final String STRETCHING = "STRETCHING";
    public static final String ISOLATION = "ISOLATION";
    public static final String OS_RESETS = "OS_RESETS";
    public static final String OTHERS = "OTHERS";
}
