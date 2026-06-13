package com.ragnarok.ragnarok_customers_training_diary.account;

import java.util.List;

/**
 * Doporučení pro rozvoj kardiorespirační kapacity (ScoutMeto kolo 5).
 * Pět tréninkových zón odvozených z maximální tepové frekvence (MTF). Procentní
 * rozpětí je fixní, konkrétní tepové rozpětí (BPM) se počítá pro daného uživatele.
 *
 * @param title       název zóny
 * @param percentText procentní rozpětí MTF (např. „60–70 %")
 * @param bpmFrom     spodní hranice tepů/min pro tohoto uživatele
 * @param bpmTo       horní hranice tepů/min pro tohoto uživatele
 * @param feeling     pocitový popis
 * @param duration    doporučená doba trvání
 * @param colorVar    CSS proměnná pro barevné odlišení zóny
 */
public record CardioZone(
        String title,
        String percentText,
        int bpmFrom,
        int bpmTo,
        String feeling,
        String duration,
        String colorVar) {

    /** Tepové rozpětí ve tvaru „114–133", k zobrazení tučně v závorce. */
    public String bpmRange() {
        return bpmFrom + "–" + bpmTo + " tepů/min";
    }

    /**
     * Sestaví 4 doporučené zóny (2–5) pro danou maximální tepovou frekvenci.
     * Hranice se zaokrouhlují na celé tepy.
     */
    public static List<CardioZone> forMaxHeartRate(int mtf) {
        return List.of(
                new CardioZone(
                        "Rozvoj základní vytrvalosti (Zóna zdraví / Zóna 2)",
                        "60–70 %", pct(mtf, 60), pct(mtf, 70),
                        "Velmi lehká, komfortní zátěž. Jsi schopen/schopna plynule mluvit celými větami "
                                + "a dýchat výhradně nosem. Svaly nepálí, cítíš se uvolněně.",
                        "45–120 minut", "--rk-success"),
                new CardioZone(
                        "Rozvoj aerobní vytrvalosti a kapacity (Zóna 3)",
                        "70–80 %", pct(mtf, 70), pct(mtf, 80),
                        "Středně těžká zátěž. Dýchání je hlubší, při mluvení se musíš nadechnout po každých "
                                + "3–5 slovech. Cítíš, že pracuješ, ale zvládneš v tomto tempu běžet nebo jet dlouho.",
                        "30–90 minut", "--rk-info"),
                new CardioZone(
                        "Zvyšování ANP – anaerobního prahu (Zóna 4)",
                        "80–90 %", pct(mtf, 80), pct(mtf, 90),
                        "Těžká až velmi těžká zátěž. Dýcháš těžce a otevřenými ústy. Nastupuje svalové pálení "
                                + "(hromadění laktátu), mluvení je možné pouze po úsecích jednoho či dvou slov.",
                        "10–40 minut (ideálně formou intervalového tréninku)", "--rk-warning"),
                new CardioZone(
                        "Maximální výkon a tolerance laktátu (Zóna 5)",
                        "90–100 %", pct(mtf, 90), pct(mtf, 100),
                        "Maximální vyčerpání. Lze udržet jen po velmi krátkou dobu, dýchání je lapavé, "
                                + "svaly zcela odmítají spolupracovat.",
                        "1–5 minut (opakované krátké intervaly)", "--rk-danger"));
    }

    private static int pct(int mtf, int percent) {
        return (int) Math.round(mtf * percent / 100.0);
    }
}
