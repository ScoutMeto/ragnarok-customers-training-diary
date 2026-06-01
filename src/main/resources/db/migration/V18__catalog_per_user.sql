-- Phase 17: per-user katalog cviků + rozšířené popisy.
--
-- Model už má: name, body_region, movement_pattern, primary_muscle, equipment,
-- description, is_system, created_by_id, active. Per-user viditelnost řešíme
-- přes is_system + created_by_id (klient vidí system + svoje vlastní).
--
-- Přidáváme jen secondary_muscles (zapojené/vedlejší svaly) pro bohatší popis.

ALTER TABLE exercise_catalog_item
    ADD COLUMN secondary_muscles VARCHAR(255);

-- Index pro filtraci "viditelné pro uživatele" (system NEBO moje vlastní).
CREATE INDEX idx_catalog_created_by ON exercise_catalog_item(created_by_id)
    WHERE created_by_id IS NOT NULL;

COMMENT ON COLUMN exercise_catalog_item.secondary_muscles IS
    'Phase 17: zapojené/vedlejší svalové skupiny (volný text, doplňuje primary_muscle).';
COMMENT ON COLUMN exercise_catalog_item.is_system IS
    'true = systémový cvik (seed nebo admin), vidí všichni. false = custom klienta, vidí jen autor.';
