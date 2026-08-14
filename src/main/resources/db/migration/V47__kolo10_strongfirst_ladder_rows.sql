-- ScoutMeto kolo 10: StrongFirst žebřík dostává generovanou (a editovatelnou) tabulku sérií.
--
-- Metodika: žebřík = série 1, 2, 3, … až po vrchol, mezi sériemi pauza. Po dosažení
-- vrcholu se začíná znovu od 1 — a takových žebříků se v tréninku odjede obvykle 3–5
-- (výchozí tabulka je na 5). Váha zůstává stejná; žebřík může být i izometrický
-- nebo „Nošení", takže místo opakování mohou být sekundy nebo metry.

ALTER TABLE strongfirst_ladder_config ADD COLUMN rep_unit VARCHAR(10);
ALTER TABLE strongfirst_ladder_config ADD CONSTRAINT sf_ladder_rep_unit_chk
    CHECK (rep_unit IS NULL OR rep_unit IN ('REPS', 'METERS', 'SECONDS'));

CREATE TABLE strongfirst_ladder_row (
    id            BIGSERIAL PRIMARY KEY,
    config_id     BIGINT    NOT NULL REFERENCES strongfirst_ladder_config(training_exercise_id) ON DELETE CASCADE,
    row_index     INT       NOT NULL,   -- 0-based pořadí řádku v celé tabulce
    ladder_index  INT       NOT NULL,   -- 1-based pořadí žebříku
    rung          INT       NOT NULL,   -- příčka (1, 2, 3, …)
    actual_value  INT,                  -- opakování / sekundy / metry dle rep_unit
                                        -- ("value" je v H2 rezervované slovo)
    weight_kg     NUMERIC(7, 2),
    rest_seconds  INT,
    side          VARCHAR(1),           -- L / P u unilaterálních cviků, jinak NULL
    CONSTRAINT sf_ladder_row_unique UNIQUE (config_id, row_index),
    CONSTRAINT sf_ladder_row_side_chk CHECK (side IS NULL OR side IN ('L', 'P'))
);
CREATE INDEX idx_sf_ladder_row ON strongfirst_ladder_row (config_id, row_index);
