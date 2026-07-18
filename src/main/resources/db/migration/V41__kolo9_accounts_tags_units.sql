-- ScoutMeto kolo 9: účty (Zrodit vikinga), jednotky per circuit krok, tag Isometrie,
-- počeštění systémových tagů.

-- 1) Nový účet musí admin nejdřív aktivovat („Zrodit vikinga"). Do té doby se
--    uživatel nepřihlásí. Existující účty jsou schválené zpětně.
ALTER TABLE account ADD COLUMN approved_at TIMESTAMP;
UPDATE account SET approved_at = NOW();

-- 2) Circuit krok: jednotka záznamu (Carry/Isometrie) — NULL = opakování
ALTER TABLE circuit_step ADD COLUMN rep_unit VARCHAR(10);
ALTER TABLE circuit_step ADD CONSTRAINT circuit_step_rep_unit_chk
    CHECK (rep_unit IS NULL OR rep_unit IN ('REPS', 'METERS', 'SECONDS'));

-- 3) Systémový tag Isometrie (sdílený pool trénink + cvik; u cviku aktivuje
--    volbu jednotky Opakování/Sekundy — statická výdrž, metry se nepřekonávají)
INSERT INTO training_tag (name, color, is_system, owner_id)
VALUES ('Isometrie', '#455A64', TRUE, NULL);

-- 4) Počeštění systémových tagů (Kettlebell, OS Resets a Carry zůstávají —
--    zavedené termíny; Carry je navíc technický spouštěč jednotek v UI)
UPDATE training_tag SET name = 'Síla a kondice' WHERE name = 'Strength & Cardio' AND is_system = TRUE;
UPDATE training_tag SET name = 'Vlastní váha'   WHERE name = 'Bodyweight'        AND is_system = TRUE;
UPDATE training_tag SET name = 'Kardio'         WHERE name = 'Cardio'            AND is_system = TRUE;
UPDATE training_tag SET name = 'Mobilita'       WHERE name = 'Mobility'          AND is_system = TRUE;
UPDATE training_tag SET name = 'Síla'           WHERE name = 'Strength'          AND is_system = TRUE;
UPDATE training_tag SET name = 'Kondice'        WHERE name = 'Conditioning'      AND is_system = TRUE;
UPDATE training_tag SET name = 'Střed těla'     WHERE name = 'Core'              AND is_system = TRUE;
UPDATE training_tag SET name = 'Celé tělo'      WHERE name = 'Fullbody'          AND is_system = TRUE;
