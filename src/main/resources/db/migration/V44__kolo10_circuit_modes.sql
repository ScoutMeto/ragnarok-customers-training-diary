-- ScoutMeto kolo 10: SUPERSET a COMPLEX přestávají být samostatné typy provedení
-- a stávají se režimem kruhového tréninku (CIRCUIT/SUPERSET/COMPLEX).
--
-- Důvod: všechny tři jsou „sled cviků × N kol", liší se jen pravidly pro pauzy
-- a sdílení náčiní. Dvě paralelní datové struktury (circuit_* vs composite_*)
-- pro totéž znamenaly dvojí UI i dvojí statistiky.

-- 1) Režim na circuit configu
ALTER TABLE circuit_config ADD COLUMN mode VARCHAR(16) NOT NULL DEFAULT 'CIRCUIT';
ALTER TABLE circuit_config ADD CONSTRAINT circuit_config_mode_chk
    CHECK (mode IN ('CIRCUIT', 'SUPERSET', 'COMPLEX'));

-- 2) Převod existujících composite záznamů na circuit.
--    circuit_config i composite_set_config mají PK = training_exercise_id,
--    takže id kroků i vazby zůstávají konzistentní.
INSERT INTO circuit_config (training_exercise_id, rounds, rest_between_rounds_s, notes, mode)
SELECT c.training_exercise_id, c.rounds, c.rest_between_rounds_s, c.notes, te.type
FROM composite_set_config c
JOIN training_exercise te ON te.id = c.training_exercise_id
WHERE te.type IN ('SUPERSET', 'COMPLEX');

-- Kroky: u COMPLEXu se sdílená váha propíše do každého kroku, který svou nemá
-- (v novém modelu drží váhu krok, sdílené pole už neexistuje).
INSERT INTO circuit_step (circuit_config_id, order_index, name, reps, weight_kg,
                          rest_seconds, note, equipment_count)
SELECT s.composite_config_id, s.order_index, s.name, s.reps,
       COALESCE(s.weight_kg, c.shared_weight_kg), s.rest_after_seconds, s.note, 1
FROM composite_set_step s
JOIN composite_set_config c ON c.training_exercise_id = s.composite_config_id
JOIN training_exercise te   ON te.id = c.training_exercise_id
WHERE te.type IN ('SUPERSET', 'COMPLEX');

UPDATE training_exercise SET type = 'CIRCUIT' WHERE type IN ('SUPERSET', 'COMPLEX');

-- 3) Composite struktura je po převodu prázdná a nikdo ji nečte → pryč.
DROP TABLE composite_set_step;
DROP TABLE composite_set_config;

-- 4) Pauza mezi koly se nově zadává v minutách + sekundách; ukládáme dál
--    v sekundách (rest_between_rounds_s), UI si to rozpadne. Per-kolo pauzy
--    už tabulku mají (circuit_round_rest z V8).
