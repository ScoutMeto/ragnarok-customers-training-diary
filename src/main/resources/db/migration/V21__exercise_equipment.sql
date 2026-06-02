-- Phase 11 (A14): per-exercise náčiní/nářadí + váha + 1/2 zátěže.

-- =====================================================================
-- 1) equipment_option — číselník pomůcek (system defaults + custom per-user)
-- =====================================================================
CREATE TABLE equipment_option (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,
    owner_id    BIGINT REFERENCES account(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_equipment_option_owner ON equipment_option(owner_id) WHERE owner_id IS NOT NULL;
-- Stejný název nesmí mít user 2× (system jsou owner NULL — řešeno aplikačně)
CREATE UNIQUE INDEX uq_equipment_option_owner_name ON equipment_option(owner_id, LOWER(name)) WHERE owner_id IS NOT NULL;

-- Default 3 system pomůcky (dle ScoutMeto A14)
INSERT INTO equipment_option (name, is_system, owner_id) VALUES
('Bez pomůcek', TRUE, NULL),
('Kettlebell',  TRUE, NULL),
('Osa',         TRUE, NULL);

-- =====================================================================
-- 2) Per-exercise equipment pole
-- =====================================================================
ALTER TABLE training_exercise
    ADD COLUMN equipment_name           VARCHAR(64),
    ADD COLUMN equipment_weight_kg      NUMERIC(7,2),
    ADD COLUMN equipment_count          SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN equipment_second_weight_kg NUMERIC(7,2);

COMMENT ON COLUMN training_exercise.equipment_name IS 'Phase 11 (A14): použité náčiní (z equipment_option nebo vlastní).';
COMMENT ON COLUMN training_exercise.equipment_count IS '1 = jedna zátěž, 2 = dvě zátěže současně (např. 2 kettlebelly).';
COMMENT ON COLUMN training_exercise.equipment_second_weight_kg IS 'Váha druhé zátěže (pokud count=2 a liší se od první).';
