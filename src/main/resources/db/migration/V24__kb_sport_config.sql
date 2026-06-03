-- Phase 13 (A12): KB sport time — config + intervaly (podrobný záznam).

CREATE TABLE kb_sport_config (
    training_exercise_id   BIGINT PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    total_seconds          INTEGER NOT NULL,
    total_reps             INTEGER,
    split_interval_seconds INTEGER,
    weight_kg              NUMERIC(6,2),
    unilateral             BOOLEAN NOT NULL DEFAULT FALSE,
    notes                  TEXT
);

CREATE TABLE kb_sport_interval (
    id                  BIGSERIAL PRIMARY KEY,
    kb_sport_config_id  BIGINT NOT NULL REFERENCES kb_sport_config(training_exercise_id) ON DELETE CASCADE,
    interval_index      INTEGER NOT NULL,
    reps                INTEGER,
    side                VARCHAR(1),
    note                VARCHAR(255)
);

CREATE INDEX idx_kb_sport_interval_config ON kb_sport_interval(kb_sport_config_id);

COMMENT ON TABLE kb_sport_config IS 'Phase 13 (A12): počet opakování za čas + volitelný split na intervaly + L/P.';
