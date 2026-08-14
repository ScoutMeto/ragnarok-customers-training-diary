-- ScoutMeto kolo 10: cvik v katalogu může mít VÍC oblastí těla a VÍC pohybových vzorců.
-- (Např. dřep s výskokem = Dolní část těla + Střed těla; Turkish Get-Up = víc vzorců.)
-- Platí i pro systémové cviky, které dosud měly natvrdo jednu hodnotu ze seedu V3.

CREATE TABLE exercise_catalog_body_region (
    catalog_item_id BIGINT      NOT NULL REFERENCES exercise_catalog_item(id) ON DELETE CASCADE,
    body_region     VARCHAR(32) NOT NULL,
    PRIMARY KEY (catalog_item_id, body_region)
);
CREATE INDEX idx_catalog_body_region_item ON exercise_catalog_body_region(catalog_item_id);

CREATE TABLE exercise_catalog_movement_pattern (
    catalog_item_id  BIGINT      NOT NULL REFERENCES exercise_catalog_item(id) ON DELETE CASCADE,
    movement_pattern VARCHAR(64) NOT NULL,
    PRIMARY KEY (catalog_item_id, movement_pattern)
);
CREATE INDEX idx_catalog_movement_pattern_item ON exercise_catalog_movement_pattern(catalog_item_id);

-- Převod stávajících jednohodnotových sloupců (nic se neztratí)
INSERT INTO exercise_catalog_body_region (catalog_item_id, body_region)
SELECT id, body_region FROM exercise_catalog_item WHERE body_region IS NOT NULL;

INSERT INTO exercise_catalog_movement_pattern (catalog_item_id, movement_pattern)
SELECT id, movement_pattern FROM exercise_catalog_item WHERE movement_pattern IS NOT NULL;

-- Původní sloupce ruším, aby nevznikly dva zdroje pravdy.
ALTER TABLE exercise_catalog_item DROP COLUMN body_region;
ALTER TABLE exercise_catalog_item DROP COLUMN movement_pattern;
