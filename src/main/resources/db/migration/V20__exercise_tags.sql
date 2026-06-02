-- Phase 11 (A2): per-exercise tagy — na co je cvik zaměřený.
-- Reuse training_tag pool (sdílené s tréninkovými tagy). Nová M:N link tabulka.

CREATE TABLE training_exercise_tag_link (
    training_exercise_id BIGINT NOT NULL REFERENCES training_exercise(id) ON DELETE CASCADE,
    tag_id               BIGINT NOT NULL REFERENCES training_tag(id) ON DELETE CASCADE,
    PRIMARY KEY (training_exercise_id, tag_id)
);

CREATE INDEX idx_ex_tag_link_exercise ON training_exercise_tag_link(training_exercise_id);
CREATE INDEX idx_ex_tag_link_tag ON training_exercise_tag_link(tag_id);

-- Nové systémové tagy zaměření (pro cviky i tréninky — sdílený pool).
INSERT INTO training_tag (name, color, is_system, owner_id) VALUES
('Core',               '#6A1B9A', TRUE, NULL),
('Fullbody',           '#00695C', TRUE, NULL),
('Horní část těla',    '#1565C0', TRUE, NULL),
('Dolní část těla',    '#2E7D32', TRUE, NULL),
('Unilaterální',       '#EF6C00', TRUE, NULL),
('Bilaterální',        '#5D4037', TRUE, NULL);
