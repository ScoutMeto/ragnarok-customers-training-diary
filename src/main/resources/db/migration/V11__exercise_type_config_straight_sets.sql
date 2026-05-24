-- ============================================================================
-- V11: Straight Sets — uniformní sety (např. 3×8, 5×5)
--
-- Jednoduché schéma: N setů s identickou strukturou. Sloužím jako alternativa
-- k FREEFORM (kde každý set může mít jiná čísla); STRAIGHT_SETS explicitně
-- vyjadřuje uniformity.
-- ============================================================================

CREATE TABLE straight_sets_config (
    training_exercise_id  BIGINT      PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,
    set_count             INT         NOT NULL,
    reps_per_set          INT,
    weight_kg             NUMERIC(6, 2),
    rest_seconds          INT,
    notes                 TEXT,
    CONSTRAINT straight_set_count_check CHECK (set_count BETWEEN 1 AND 50)
);
