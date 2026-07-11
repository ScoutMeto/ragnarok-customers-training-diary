-- ScoutMeto kolo 8: přepracování formulářů typů cviků + tag Carry.
--
-- 1) KB sport time: podrobný záznam po částech (handswitch / bilaterální) —
--    části mají navíc trvání (min+s ukládáme v sekundách).
-- 2) Circuit: náčiní + tagy per krok kruhového tréninku.
-- 3) Freeform sety: sloupec Odpočinek (s) + jednotka Carry (metry/sekundy).
-- 4) Ladder/Stepladder/Pyramid: per-řádkové uložení vygenerované série.
-- 5) Typ CORE se ruší → existující záznamy převést na FREEFORM.
-- 6) Nový systémový tag Carry.

-- 1) KB sport interval = "část" podrobného rozpisu; nově s trváním
ALTER TABLE kb_sport_interval ADD COLUMN duration_seconds INT;

-- 2) Circuit step: vlastní náčiní (stejná pole jako u cviku) + tagy
ALTER TABLE circuit_step ADD COLUMN equipment_name VARCHAR(64);
ALTER TABLE circuit_step ADD COLUMN equipment_weight_kg NUMERIC(7,2);
ALTER TABLE circuit_step ADD COLUMN equipment_count INT NOT NULL DEFAULT 1;
ALTER TABLE circuit_step ADD COLUMN equipment_second_weight_kg NUMERIC(7,2);

CREATE TABLE circuit_step_tag_link (
    circuit_step_id BIGINT NOT NULL REFERENCES circuit_step(id) ON DELETE CASCADE,
    tag_id          BIGINT NOT NULL REFERENCES training_tag(id) ON DELETE CASCADE,
    PRIMARY KEY (circuit_step_id, tag_id)
);
CREATE INDEX idx_circuit_step_tag_link_step ON circuit_step_tag_link(circuit_step_id);
CREATE INDEX idx_circuit_step_tag_link_tag ON circuit_step_tag_link(tag_id);

-- 3) Sety: odpočinek po setu + jednotka záznamu (NULL = opakování; METERS/SECONDS pro Carry)
ALTER TABLE exercise_set ADD COLUMN rest_seconds INT;
ALTER TABLE training_exercise ADD COLUMN set_unit VARCHAR(10);
ALTER TABLE training_exercise ADD CONSTRAINT training_exercise_set_unit_chk
    CHECK (set_unit IS NULL OR set_unit IN ('REPS', 'METERS', 'SECONDS'));

-- 4) Ladder/Stepladder/Pyramid: vygenerovaná (a editovatelná) tabulka řádků
--    rung = číslo série/stupně, row_index = pořadí řádku v celé tabulce
CREATE TABLE numeric_series_row (
    id           BIGSERIAL PRIMARY KEY,
    config_id    BIGINT NOT NULL REFERENCES numeric_series_config(training_exercise_id) ON DELETE CASCADE,
    row_index    INT NOT NULL,
    rung         INT,
    reps         INT,
    weight_kg    NUMERIC(6,2)
);
CREATE INDEX idx_numeric_series_row_config ON numeric_series_row(config_id);

-- 5) CORE typ se ruší (Scout: vymazat tento typ provedení)
UPDATE training_exercise SET type = 'FREEFORM' WHERE type = 'CORE';

-- 6) Systémový tag Carry (aktivuje volbu jednotky metry/sekundy u setů)
INSERT INTO training_tag (name, color, is_system, owner_id)
VALUES ('Carry', '#8D6E63', TRUE, NULL);
