-- Phase 13 (A11): Intervalový trénink — config tabulka (OneToOne shared PK).

CREATE TABLE interval_config (
    training_exercise_id BIGINT PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    rounds        INTEGER NOT NULL,
    work_reps     INTEGER,
    work_seconds  INTEGER,
    rest_seconds  INTEGER NOT NULL,
    weight_kg     NUMERIC(6,2),
    notes         TEXT
);

COMMENT ON TABLE interval_config IS 'Phase 13 (A11): N kol práce (reps NEBO čas) + pauza.';
