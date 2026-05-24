-- ============================================================================
-- V10: Composite sets — Superset + Complex
--
-- Superset: 2-4 cviky střídavě, N kol (např. squat → push-up × 5 kol)
-- Complex:  2-4 cviky se stejnou činkou v řadě, N kol (např. KB clean→press→squat)
--
-- Sdílená tabulka, training_exercise.type rozliší. Pro Complex existuje navíc
-- shared_weight_kg (jedna činka pro celou kompozici).
-- ============================================================================

CREATE TABLE composite_set_config (
    training_exercise_id  BIGINT      PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    rounds                INT         NOT NULL DEFAULT 3,
    shared_weight_kg      NUMERIC(6, 2),
    rest_between_rounds_s INT,
    notes                 TEXT,
    CONSTRAINT composite_rounds_check CHECK (rounds BETWEEN 1 AND 30)
);

CREATE TABLE composite_set_step (
    id                    BIGSERIAL   PRIMARY KEY,
    composite_config_id   BIGINT      NOT NULL REFERENCES composite_set_config(training_exercise_id) ON DELETE CASCADE,
    order_index           INT         NOT NULL,
    name                  VARCHAR(128) NOT NULL,
    reps                  INT,
    weight_kg             NUMERIC(6, 2),
    rest_after_seconds    INT,
    note                  VARCHAR(255)
);

CREATE INDEX idx_composite_step ON composite_set_step (composite_config_id, order_index);
