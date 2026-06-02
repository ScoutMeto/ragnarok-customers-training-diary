-- Phase 13 (A10): StrongFirst žebřík — config tabulka (OneToOne shared PK).

CREATE TABLE strongfirst_ladder_config (
    training_exercise_id BIGINT PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    ladder_height INTEGER NOT NULL,
    cycles        INTEGER NOT NULL DEFAULT 1,
    rest_seconds  INTEGER,
    weight_kg     NUMERIC(6,2),
    unilateral    BOOLEAN NOT NULL DEFAULT FALSE,
    notes         TEXT
);

COMMENT ON TABLE strongfirst_ladder_config IS
    'Phase 13 (A10): StrongFirst žebřík 1,2,...,ladder_height reps + pauza, opakuj cycles×.';
