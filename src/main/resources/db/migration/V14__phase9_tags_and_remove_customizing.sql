-- Phase 9: nové system tagy (A15) + odstranění typu CUSTOMIZING (A5).

-- =====================================================================
-- A15: Nové systémové tagy
-- =====================================================================
-- ISOLATION, OTHERS, STRETCHING, BARBELL. Vlastní (custom) tagy si klient
-- přidává v UI (owner_id NOT NULL).
INSERT INTO training_tag (name, color, is_system, owner_id) VALUES
('Isolation',   '#8E24AA', TRUE, NULL),
('Stretching',  '#00ACC1', TRUE, NULL),
('Barbell',     '#5D4037', TRUE, NULL),
('Others',      '#607D8B', TRUE, NULL);

-- =====================================================================
-- A5: CUSTOMIZING je totožný s FREEFORM → sloučit
-- =====================================================================
-- Přepíšeme existující záznamy na FREEFORM. Enum hodnota CUSTOMIZING
-- se odstraňuje z Javy. Sloupec training_exercise.type je VARCHAR bez
-- CHECK constraintu, takže stačí UPDATE.
UPDATE training_exercise SET type = 'FREEFORM' WHERE type = 'CUSTOMIZING';
