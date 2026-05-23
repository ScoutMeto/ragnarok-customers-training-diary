-- ============================================================================
-- V7: Time-based exercise types — EMOM, Tabata, AMRAP
--
-- Každý typ má svou config tabulku 1:1 s training_exercise (sdílí PK).
-- Per-interval overrides drží sub-tabulka (jen tam, kde dává smysl).
--
-- Convention:
--   - {type}_config.training_exercise_id PRIMARY KEY + FK na training_exercise(id)
--   - mazat config se kaskáduje přes DELETE CASCADE
-- ============================================================================


-- =============================================================================
-- EMOM (Every Minute On the Minute)
--   "10 minut, každou minutu udělej 5 swingů s 24 kg"
-- =============================================================================
CREATE TABLE emom_config (
    training_exercise_id  BIGINT      PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    total_minutes         INT         NOT NULL,
    interval_seconds      INT         NOT NULL DEFAULT 60,
    default_reps          INT,
    default_weight_kg     NUMERIC(6, 2),
    notes                 TEXT,
    CONSTRAINT emom_total_minutes_check  CHECK (total_minutes BETWEEN 1 AND 120),
    CONSTRAINT emom_interval_check       CHECK (interval_seconds BETWEEN 1 AND 600)
);

CREATE TABLE emom_minute_override (
    id                    BIGSERIAL   PRIMARY KEY,
    emom_config_id        BIGINT      NOT NULL REFERENCES emom_config(training_exercise_id) ON DELETE CASCADE,
    minute_index          INT         NOT NULL,    -- 1-based: 1, 2, ..., total_minutes
    reps                  INT,
    weight_kg             NUMERIC(6, 2),
    note                  VARCHAR(255),
    CONSTRAINT emom_minute_unique UNIQUE (emom_config_id, minute_index)
);


-- =============================================================================
-- Tabata (8 × 20s work / 10s rest, pevné schema)
--   Konfigurovatelně přes rounds + work + rest pro variace.
-- =============================================================================
CREATE TABLE tabata_config (
    training_exercise_id  BIGINT      PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    rounds                INT         NOT NULL DEFAULT 8,
    work_seconds          INT         NOT NULL DEFAULT 20,
    rest_seconds          INT         NOT NULL DEFAULT 10,
    default_reps          INT,
    default_weight_kg     NUMERIC(6, 2),
    notes                 TEXT,
    CONSTRAINT tabata_rounds_check CHECK (rounds BETWEEN 1 AND 30),
    CONSTRAINT tabata_work_check   CHECK (work_seconds BETWEEN 5 AND 300),
    CONSTRAINT tabata_rest_check   CHECK (rest_seconds BETWEEN 0 AND 300)
);

CREATE TABLE tabata_round_override (
    id                    BIGSERIAL   PRIMARY KEY,
    tabata_config_id      BIGINT      NOT NULL REFERENCES tabata_config(training_exercise_id) ON DELETE CASCADE,
    round_index           INT         NOT NULL,    -- 1-based
    reps                  INT,
    weight_kg             NUMERIC(6, 2),
    note                  VARCHAR(255),
    CONSTRAINT tabata_round_unique UNIQUE (tabata_config_id, round_index)
);


-- =============================================================================
-- AMRAP (As Many Rounds As Possible v daném časovém limitu)
--   "8 minut, kolik kol stihneš"
-- =============================================================================
CREATE TABLE amrap_config (
    training_exercise_id  BIGINT      PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    timecap_seconds       INT         NOT NULL,
    target_reps_per_round INT,                      -- nepovinné: očekávaný plán reps na kolo
    target_weight_kg      NUMERIC(6, 2),
    rounds_completed      INT,                      -- logování výsledku (kolik kol klient stihl)
    extra_reps            INT,                      -- výsledek: navíc reps poslední (nedokončené) kolo
    notes                 TEXT,
    CONSTRAINT amrap_timecap_check CHECK (timecap_seconds BETWEEN 30 AND 7200)
);
