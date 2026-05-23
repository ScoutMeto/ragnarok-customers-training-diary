-- ============================================================================
-- V2: Tréninkový deník — datový model
--
-- Hierarchie:
--   account (V1)
--     └─ training (1:N, 1 trénink = 1 klient)
--           └─ training_exercise (1:N, cviky v daném pořadí)
--                  └─ exercise_set (1:N, jednotlivé série cviku)
--
--   exercise_catalog_item — globální katalog cviků (system seed + custom per admin)
--   training_tag           — tagy pro štítkování tréninků (system + per-user custom)
--   training_tag_link      — M:N mezi training a training_tag
-- ============================================================================


-- =============================================================================
-- Katalog cviků
-- =============================================================================

CREATE TABLE exercise_catalog_item (
    id                  BIGSERIAL    PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    body_region         VARCHAR(32),       -- FULL_BODY, UPPER_BODY, LOWER_BODY, CORE
    movement_pattern    VARCHAR(32),       -- PUSH, PULL, SQUAT, HINGE, LUNGE, ROTATION, CARRY, GAIT, ISOMETRIC, PLYO, OTHER
    primary_muscle      VARCHAR(64),       -- volnotextové (např. "quadriceps", "lats")
    equipment           VARCHAR(64),       -- BARBELL, KETTLEBELL, DUMBBELL, BODYWEIGHT, BAND, MACHINE, CABLE, NONE
    description         TEXT,
    is_system           BOOLEAN      NOT NULL DEFAULT TRUE,    -- TRUE = seed, FALSE = vytvořený adminem
    created_by_id       BIGINT       REFERENCES account(id) ON DELETE SET NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_catalog_name              ON exercise_catalog_item (LOWER(name));
CREATE INDEX idx_catalog_body_region       ON exercise_catalog_item (body_region);
CREATE INDEX idx_catalog_movement_pattern  ON exercise_catalog_item (movement_pattern);
CREATE INDEX idx_catalog_active            ON exercise_catalog_item (active);


-- =============================================================================
-- Tagy
-- =============================================================================

CREATE TABLE training_tag (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(64)  NOT NULL,
    color         VARCHAR(16),                -- HEX (např. "#FF5722"), volitelně pro UI badge
    is_system     BOOLEAN      NOT NULL DEFAULT FALSE,    -- system = předefinovaný, owner_id IS NULL
    owner_id      BIGINT       REFERENCES account(id) ON DELETE CASCADE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    -- system tagy nemají owner; custom tagy mají vždy owner
    CONSTRAINT tag_system_xor_owner CHECK (
        (is_system = TRUE  AND owner_id IS NULL) OR
        (is_system = FALSE AND owner_id IS NOT NULL)
    ),
    -- unikátní per scope: system tagy unikátní globálně, custom unikátní per uživatel
    CONSTRAINT tag_unique_per_scope UNIQUE (owner_id, name)
);

CREATE INDEX idx_tag_owner    ON training_tag (owner_id);
CREATE INDEX idx_tag_system   ON training_tag (is_system) WHERE is_system = TRUE;


-- =============================================================================
-- Trénink
-- =============================================================================

CREATE TABLE training (
    id              BIGSERIAL    PRIMARY KEY,
    owner_id        BIGINT       NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    training_date   DATE         NOT NULL,
    start_time      TIME,
    end_time        TIME,
    name            VARCHAR(128),
    difficulty      VARCHAR(16),         -- LIGHT, MEDIUM, HARD (nullable, klient sám)
    rpe             SMALLINT,            -- 1-10
    notes           TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT training_difficulty_check CHECK (difficulty IS NULL OR difficulty IN ('LIGHT', 'MEDIUM', 'HARD')),
    CONSTRAINT training_rpe_check        CHECK (rpe IS NULL OR (rpe BETWEEN 1 AND 10))
);

CREATE INDEX idx_training_owner_date ON training (owner_id, training_date DESC);


-- =============================================================================
-- Cvik v tréninku
-- =============================================================================

CREATE TABLE training_exercise (
    id              BIGSERIAL    PRIMARY KEY,
    training_id     BIGINT       NOT NULL REFERENCES training(id) ON DELETE CASCADE,
    order_index     INT          NOT NULL,
    type            VARCHAR(32)  NOT NULL,    -- FREEFORM, CUSTOMIZING, EMOM, CIRCUIT, TABATA, AMRAP, ...
    catalog_item_id BIGINT       REFERENCES exercise_catalog_item(id) ON DELETE SET NULL,
    custom_name     VARCHAR(128),
    rpe             SMALLINT,
    notes           TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    -- XOR: buď odkaz na katalog, nebo vlastní název
    CONSTRAINT exercise_name_xor CHECK (
        (catalog_item_id IS NOT NULL AND custom_name IS NULL) OR
        (catalog_item_id IS NULL     AND custom_name IS NOT NULL)
    ),
    CONSTRAINT exercise_rpe_check CHECK (rpe IS NULL OR (rpe BETWEEN 1 AND 10))
);

CREATE INDEX idx_exercise_training ON training_exercise (training_id, order_index);


-- =============================================================================
-- Série (sety) cviku
-- =============================================================================

CREATE TABLE exercise_set (
    id                      BIGSERIAL    PRIMARY KEY,
    training_exercise_id    BIGINT       NOT NULL REFERENCES training_exercise(id) ON DELETE CASCADE,
    set_index               INT          NOT NULL,
    weight_kg               NUMERIC(6, 2),    -- nullable, pro tělesnou váhu nebo plyo
    reps                    INT,
    rpe                     SMALLINT,
    note                    VARCHAR(255),
    CONSTRAINT set_rpe_check CHECK (rpe IS NULL OR (rpe BETWEEN 1 AND 10))
);

CREATE INDEX idx_set_exercise ON exercise_set (training_exercise_id, set_index);


-- =============================================================================
-- M:N: trénink × tag
-- =============================================================================

CREATE TABLE training_tag_link (
    training_id     BIGINT NOT NULL REFERENCES training(id)     ON DELETE CASCADE,
    tag_id          BIGINT NOT NULL REFERENCES training_tag(id) ON DELETE CASCADE,
    PRIMARY KEY (training_id, tag_id)
);

CREATE INDEX idx_tag_link_tag ON training_tag_link (tag_id);
