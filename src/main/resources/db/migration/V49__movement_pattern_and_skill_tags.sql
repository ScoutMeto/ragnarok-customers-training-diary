-- Kolo 10+: strukturované sledování výšky výskoku pro hlavní cviky a složené kroky.
ALTER TABLE training_exercise ADD COLUMN IF NOT EXISTS jump_height_cm NUMERIC(6,2);
ALTER TABLE circuit_step ADD COLUMN IF NOT EXISTS jump_height_cm NUMERIC(6,2);
ALTER TABLE amrap_step ADD COLUMN IF NOT EXISTS jump_height_cm NUMERIC(6,2);
-- Kolo 10+: systémové tagy pro pohybové vzorce a nové sledované dovednosti.
-- Logika aplikace se řídí system_key, český název je pouze uživatelský popisek.

-- Doplň české názvy tam, kde už systémový klíč existuje.
UPDATE training_tag SET name = 'Nošení' WHERE system_key = 'CARRY';
UPDATE training_tag SET name = 'Isometrie' WHERE system_key = 'ISOMETRY';
UPDATE training_tag SET name = 'Jiné' WHERE system_key = 'OTHERS';

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Běh', '#1976D2', TRUE, NULL, 'GAIT'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'GAIT');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Kyčelní ohyb', '#5D4037', TRUE, NULL, 'HINGE'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'HINGE');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Výpad', '#689F38', TRUE, NULL, 'LUNGE'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'LUNGE');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Plyometrie', '#F57C00', TRUE, NULL, 'PLYO'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'PLYO');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Tah', '#00838F', TRUE, NULL, 'PULL'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'PULL');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Tlak', '#9F371B', TRUE, NULL, 'PUSH'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'PUSH');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Rotace', '#7B1FA2', TRUE, NULL, 'ROTATION'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'ROTATION');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Dřep', '#455A64', TRUE, NULL, 'SQUAT'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'SQUAT');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Lokomoce a animal movements', '#2E7D32', TRUE, NULL, 'LOCOMOTION'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'LOCOMOTION');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Skoky, výskoky', '#C2185B', TRUE, NULL, 'JUMPS'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'JUMPS');

INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
SELECT 'Koordinace', '#6A1B9A', TRUE, NULL, 'COORDINATION'
WHERE NOT EXISTS (SELECT 1 FROM training_tag WHERE system_key = 'COORDINATION');