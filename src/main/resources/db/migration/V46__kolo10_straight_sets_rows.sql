-- ScoutMeto kolo 10: Straight Sets dostávají generovanou (a editovatelnou) tabulku setů.
-- Do teď se dal zapsat jen předpis „5×5 s 24 kg", ne to, co se reálně odcvičilo.

CREATE TABLE straight_sets_row (
    id                     BIGSERIAL PRIMARY KEY,
    straight_sets_config_id BIGINT   NOT NULL REFERENCES straight_sets_config(training_exercise_id) ON DELETE CASCADE,
    row_index              INT       NOT NULL,   -- 0-based pořadí setu
    reps                   INT,
    weight_kg              NUMERIC(7, 2),
    rest_seconds           INT,
    CONSTRAINT straight_sets_row_unique UNIQUE (straight_sets_config_id, row_index)
);
CREATE INDEX idx_straight_sets_row ON straight_sets_row (straight_sets_config_id, row_index);
