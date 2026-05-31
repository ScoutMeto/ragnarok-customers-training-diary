-- Phase 9 (A13): difficulty LIGHT/MEDIUM/HARD → 9 konkrétních zaměření ve 3 úrovních.
--
-- V2 mělo CHECK constraint (difficulty IN 'LIGHT','MEDIUM','HARD') + VARCHAR(16).
-- Nové hodnoty jsou jiné a delší → musíme:
--   1) dropnout starý CHECK constraint
--   2) rozšířit sloupec (nejdelší hodnota má 15 znaků, dáme rezervu)
--   3) přemapovat stará data na reprezentativní zaměření v dané úrovni
--   4) přidat nový CHECK constraint s 9 hodnotami

ALTER TABLE training DROP CONSTRAINT IF EXISTS training_difficulty_check;

ALTER TABLE training ALTER COLUMN difficulty TYPE VARCHAR(32);

UPDATE training SET difficulty = CASE difficulty
    WHEN 'LIGHT'  THEN 'LEHKY_TRENINK'
    WHEN 'MEDIUM' THEN 'SILOVE_KONDICNI'
    WHEN 'HARD'   THEN 'ROZVOJ_MAX_SILY'
    ELSE difficulty
END
WHERE difficulty IS NOT NULL;

ALTER TABLE training ADD CONSTRAINT training_difficulty_check
    CHECK (difficulty IS NULL OR difficulty IN (
        'LEHKY_TRENINK', 'DELOAD', 'RYCHLOST',
        'SILOVE_KONDICNI', 'SBER_OPAKOVANI', 'DRILL', 'VYUKA',
        'ROZVOJ_MAX_SILY', 'TESTOVANI'
    ));
