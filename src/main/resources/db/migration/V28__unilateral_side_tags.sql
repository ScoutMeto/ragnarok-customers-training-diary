-- Phase 19d (ScoutMeto): k tagu „Unilaterální" přidat varianty pro stranu.
-- „Unilaterální" zůstává (když uživatel nechce zadat stranu), navíc levá/pravá.

INSERT INTO training_tag (name, color, is_system, owner_id) VALUES
    ('Unilaterální levá',  '#EF6C00', TRUE, NULL),
    ('Unilaterální pravá', '#EF6C00', TRUE, NULL);
