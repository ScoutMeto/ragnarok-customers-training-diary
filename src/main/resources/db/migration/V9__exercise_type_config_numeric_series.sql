-- ============================================================================
-- V9: Numeric series — Ladder, Stepladder, Pyramid
--
-- Sdílená tabulka pro tři podobné typy. Konkrétní typ je v training_exercise.type;
-- tato tabulka drží jen číselné parametry.
--
-- Pattern:
--   - LADDER: 1 → peak (krok = step). Klesající ladder: start > peak.
--   - STEPLADDER: custom rep_sequence_csv (např. "1,2,3,5,8").
--   - PYRAMID: 1 → peak → 1 (automaticky symetrická).
--
-- Pokud rep_sequence_csv vyplněn, má přednost před algoritmickým generováním.
-- ============================================================================

CREATE TABLE numeric_series_config (
    training_exercise_id  BIGINT      PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    start_value           INT         DEFAULT 1,
    peak_value            INT,
    step_size             INT         DEFAULT 1,
    rep_sequence_csv      VARCHAR(255),
    weight_kg             NUMERIC(6, 2),
    rest_seconds_between  INT,
    notes                 TEXT,
    CONSTRAINT numeric_series_step_check CHECK (step_size IS NULL OR step_size > 0)
);
