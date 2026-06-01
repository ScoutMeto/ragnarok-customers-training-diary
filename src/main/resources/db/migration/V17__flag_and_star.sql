-- Phase 14: vlaječka tréninku (B8) + korunka cviku (A16).

-- B8: uživatel si označí důležitý trénink vlaječkou (per-trénink).
ALTER TABLE training
    ADD COLUMN flagged BOOLEAN NOT NULL DEFAULT FALSE;

-- A16: „korunka" — per-instance oblíbený cvik (konkrétní cvik v konkrétním tréninku).
ALTER TABLE training_exercise
    ADD COLUMN starred BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_training_flagged ON training(flagged) WHERE flagged = TRUE;
CREATE INDEX idx_training_exercise_starred ON training_exercise(starred) WHERE starred = TRUE;

COMMENT ON COLUMN training.flagged IS 'Phase 14 (B8): uživatelská vlaječka důležitého tréninku.';
COMMENT ON COLUMN training_exercise.starred IS 'Phase 14 (A16): korunka — per-instance oblíbený cvik.';
