-- ============================================================================
-- V4: Seed systémových tagů
--
-- Předdefinované tagy odpovídající původním "typům tréninku" ze starého modelu
-- (kb, strengthAndCardio, bodyweight, cardio, OS Resets, anotherTrainingType).
-- Klient si k tréninku přiřadí jeden nebo víc tagů. Vlastní (custom) tagy se
-- přidávají v UI (vždy svázané s konkrétním uživatelem).
-- ============================================================================

INSERT INTO training_tag (name, color, is_system, owner_id) VALUES
('Kettlebell',           '#D32F2F', TRUE, NULL),
('Strength & Cardio',    '#1976D2', TRUE, NULL),
('Bodyweight',           '#388E3C', TRUE, NULL),
('Cardio',               '#F57C00', TRUE, NULL),
('OS Resets',            '#7B1FA2', TRUE, NULL),
('Mobility',             '#0097A7', TRUE, NULL),
('Strength',             '#455A64', TRUE, NULL),
('Conditioning',         '#FBC02D', TRUE, NULL);
