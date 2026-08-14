-- ScoutMeto kolo 10: AMRAP je vlastně kruhový trénink bez předdefinovaných pauz —
-- opakuje se sada cviků, dokud nedojde čas. Do teď uměl jen jeden souhrnný počet
-- opakování na kolo, takže se do něj nedal zapsat reálný workout o víc cvicích.
--
-- Nově: seznam cviků (jako u CIRCUITu) + záznam po kolech včetně posledního,
-- rozjetého kola (u nedokončených cviků se dá kolo vyřadit).

CREATE TABLE amrap_step (
    id                        BIGSERIAL    PRIMARY KEY,
    amrap_config_id           BIGINT       NOT NULL REFERENCES amrap_config(training_exercise_id) ON DELETE CASCADE,
    order_index               INT          NOT NULL,
    name                      VARCHAR(128) NOT NULL,
    reps                      INT,                    -- cíl na kolo (nebo metry/sekundy dle rep_unit)
    rep_unit                  VARCHAR(10),            -- NULL = opakování; METERS/SECONDS (Nošení/Izometrie)
    weight_kg                 NUMERIC(6, 2),
    note                      VARCHAR(255),
    equipment_name            VARCHAR(64),
    equipment_weight_kg       NUMERIC(7, 2),
    equipment_count           INT          NOT NULL DEFAULT 1,
    equipment_second_weight_kg NUMERIC(7, 2),
    CONSTRAINT amrap_step_rep_unit_chk CHECK (rep_unit IS NULL OR rep_unit IN ('REPS', 'METERS', 'SECONDS'))
);
CREATE INDEX idx_amrap_step ON amrap_step (amrap_config_id, order_index);

CREATE TABLE amrap_step_tag_link (
    amrap_step_id BIGINT NOT NULL REFERENCES amrap_step(id)   ON DELETE CASCADE,
    tag_id        BIGINT NOT NULL REFERENCES training_tag(id) ON DELETE CASCADE,
    PRIMARY KEY (amrap_step_id, tag_id)
);
CREATE INDEX idx_amrap_step_tag_link_tag ON amrap_step_tag_link(tag_id);

-- Záznam po kolech. Poslední kolo bývá rozjeté: cvik, na který už nezbyl čas,
-- se označí skipped = TRUE, u rozcvičeného se zapíše skutečná hodnota.
CREATE TABLE amrap_round_entry (
    id               BIGSERIAL PRIMARY KEY,
    amrap_config_id  BIGINT    NOT NULL REFERENCES amrap_config(training_exercise_id) ON DELETE CASCADE,
    round_index      INT       NOT NULL,   -- 1-based
    step_order       INT       NOT NULL,   -- = amrap_step.order_index (0-based)
    skipped          BOOLEAN   NOT NULL DEFAULT FALSE,
    actual_reps      INTEGER,
    actual_weight_kg NUMERIC(7, 2),
    note             VARCHAR(255),
    CONSTRAINT amrap_round_entry_unique UNIQUE (amrap_config_id, round_index, step_order)
);
CREATE INDEX idx_amrap_round_entry_config ON amrap_round_entry(amrap_config_id);

-- Time-cap se nově zadává v minutách + sekundách; sloupec zůstává v sekundách.
-- Původní CHECK (30–7200 s) byl zbytečně přísný pro krátké AMRAPy.
ALTER TABLE amrap_config DROP CONSTRAINT amrap_timecap_check;
ALTER TABLE amrap_config ADD CONSTRAINT amrap_timecap_check
    CHECK (timecap_seconds BETWEEN 1 AND 14400);
