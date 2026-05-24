-- ============================================================================
-- V12: Phase 8 — TrainingTemplate (přes Training.visibility=TEMPLATE) + CoachPlan
--
-- Design:
--  1. Šablona tréninku NENÍ samostatná entita. Je to Training s visibility=TEMPLATE.
--     Sdílí veškerou strukturu cviků/setů/per-type configs. Není vidět v deníku
--     klienta ani na dashboardu.
--  2. Když admin „přiřadí" template klientovi na datum, vznikne PRIVATE Training
--     jako kopie šablony s odkazem na zdroj přes training.source_template_id.
--  3. coach_plan = textový (markdown) plán na N týdnů od trenéra konkrétnímu klientovi.
-- ============================================================================

-- Rozšíření visibility check: přidat TEMPLATE jako třetí hodnotu.
-- DROP a re-create konkrétních constraintů (PostgreSQL umí jen DROP/ADD, ne MODIFY).
ALTER TABLE training DROP CONSTRAINT training_visibility_check;
ALTER TABLE training ADD CONSTRAINT training_visibility_check CHECK (
    visibility IN ('PRIVATE', 'GROUP', 'TEMPLATE')
);

-- TEMPLATE má stejná pravidla jako GROUP — nemusí mít owner (sdílen pro budoucí
-- assignments). Ale má created_by (admin který šablonu napsal).
ALTER TABLE training DROP CONSTRAINT training_visibility_owner_check;
ALTER TABLE training ADD CONSTRAINT training_visibility_owner_check CHECK (
    (visibility = 'PRIVATE' AND owner_id IS NOT NULL) OR
    (visibility IN ('GROUP', 'TEMPLATE'))
);

-- source_template_id — FK z PRIVATE tréninku na šablonu, ze které byl odvozen.
-- Nullable: většina PRIVATE tréninků nemá zdroj (klient si je psal sám).
ALTER TABLE training ADD COLUMN source_template_id BIGINT
    REFERENCES training(id) ON DELETE SET NULL;

-- Index pro „najdi všechny instance této šablony" (např. statistiky kolik klientů
-- šablonu absolvovalo).
CREATE INDEX idx_training_source_template ON training (source_template_id)
    WHERE source_template_id IS NOT NULL;

-- Index pro admin sekci šablon (nejnovější nahoře).
CREATE INDEX idx_training_template ON training (created_at DESC)
    WHERE visibility = 'TEMPLATE';


-- =============================================================================
-- coach_plan — markdown textový plán pro klienta na období
-- =============================================================================
CREATE TABLE coach_plan (
    id              BIGSERIAL    PRIMARY KEY,
    client_id       BIGINT       NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    author_id       BIGINT       NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    title           VARCHAR(128) NOT NULL,
    body_markdown   TEXT         NOT NULL,
    valid_from      DATE         NOT NULL,
    valid_to        DATE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT coach_plan_valid_range CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

CREATE INDEX idx_coach_plan_client ON coach_plan (client_id, valid_from DESC);
