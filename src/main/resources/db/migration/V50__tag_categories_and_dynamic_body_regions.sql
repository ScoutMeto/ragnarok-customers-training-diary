-- Kategorizace tagů pro analytiku a rozšíření katalogových oblastí těla.
-- Stávající system_key zůstává stabilní zdroj chování jednotek a historických dat.

ALTER TABLE training_tag ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'GENERAL';

UPDATE training_tag
SET category = 'BODY_REGION'
WHERE system_key IN ('FULL_BODY', 'UPPER_BODY', 'LOWER_BODY', 'CORE');

UPDATE training_tag
SET category = 'MOVEMENT_PATTERN'
WHERE system_key IN ('CARRY', 'GAIT', 'HINGE', 'ISOMETRY', 'ISOMETRIC', 'LUNGE', 'OTHER', 'OTHERS',
                     'PLYO', 'PULL', 'PUSH', 'ROTATION', 'SQUAT', 'LOCOMOTION', 'JUMPS', 'COORDINATION');

UPDATE training_tag
SET category = 'EQUIPMENT'
WHERE system_key IN ('BODYWEIGHT', 'KETTLEBELL', 'BARBELL');

ALTER TABLE catalog_attribute_option ADD COLUMN owner_id BIGINT REFERENCES account(id) ON DELETE CASCADE;
ALTER TABLE catalog_attribute_option DROP CONSTRAINT uq_catalog_attr_option;
CREATE UNIQUE INDEX uq_catalog_attr_option_global ON catalog_attribute_option (kind, name) WHERE owner_id IS NULL;
CREATE UNIQUE INDEX uq_catalog_attr_option_owner ON catalog_attribute_option (kind, owner_id, name) WHERE owner_id IS NOT NULL;

ALTER TABLE exercise_catalog_body_region ALTER COLUMN body_region TYPE VARCHAR(64);

INSERT INTO catalog_attribute_option (kind, name, is_system)
SELECT 'BODY_REGION', 'FULL_BODY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM catalog_attribute_option WHERE kind = 'BODY_REGION' AND name = 'FULL_BODY');

INSERT INTO catalog_attribute_option (kind, name, is_system)
SELECT 'BODY_REGION', 'UPPER_BODY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM catalog_attribute_option WHERE kind = 'BODY_REGION' AND name = 'UPPER_BODY');

INSERT INTO catalog_attribute_option (kind, name, is_system)
SELECT 'BODY_REGION', 'LOWER_BODY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM catalog_attribute_option WHERE kind = 'BODY_REGION' AND name = 'LOWER_BODY');

INSERT INTO catalog_attribute_option (kind, name, is_system)
SELECT 'BODY_REGION', 'CORE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM catalog_attribute_option WHERE kind = 'BODY_REGION' AND name = 'CORE');