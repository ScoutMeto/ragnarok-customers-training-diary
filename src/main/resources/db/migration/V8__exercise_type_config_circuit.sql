-- ============================================================================
-- V8: Circuit — kruhový trénink s vnořenými kroky
--
-- Circuit má rounds (počet kol) a steps (cviky v jednom kole). Volitelně per-round
-- override pauz (mezi specifickými koly delší pauza apod.).
-- ============================================================================

CREATE TABLE circuit_config (
    training_exercise_id  BIGINT      PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    rounds                INT         NOT NULL DEFAULT 3,
    rest_between_rounds_s INT,
    notes                 TEXT,
    CONSTRAINT circuit_rounds_check CHECK (rounds BETWEEN 1 AND 30)
);

CREATE TABLE circuit_step (
    id                    BIGSERIAL   PRIMARY KEY,
    circuit_config_id     BIGINT      NOT NULL REFERENCES circuit_config(training_exercise_id) ON DELETE CASCADE,
    order_index           INT         NOT NULL,
    name                  VARCHAR(128) NOT NULL,
    reps                  INT,
    duration_seconds      INT,
    weight_kg             NUMERIC(6, 2),
    rest_seconds          INT,
    note                  VARCHAR(255)
);

CREATE INDEX idx_circuit_step ON circuit_step (circuit_config_id, order_index);

CREATE TABLE circuit_round_rest (
    id                    BIGSERIAL   PRIMARY KEY,
    circuit_config_id     BIGINT      NOT NULL REFERENCES circuit_config(training_exercise_id) ON DELETE CASCADE,
    round_index           INT         NOT NULL,    -- 1-based, znamená "po N-tém kole"
    rest_seconds          INT         NOT NULL,
    CONSTRAINT circuit_round_rest_unique UNIQUE (circuit_config_id, round_index)
);
