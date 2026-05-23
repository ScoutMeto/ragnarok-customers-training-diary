-- ============================================================================
-- V6: Group trainings on Training entity (drop GroupLessonPlan, add visibility)
--
-- User clarification: admin creates structured trainings (cviky + sety, same
-- shape as client's training) marked as visible to ALL clients for a given day.
-- 'Plán lekcí' as informational entity was wrong abstraction → drop.
--
-- Změny:
--   1. Drop group_lesson_plan (zavedeno v V5, nyní nepotřebné).
--   2. training.visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE'
--   3. training.owner_id -> nullable (GROUP nemá owner)
--   4. training.created_by_id BIGINT NULLABLE → kdo trénink založil
--   5. CHECK: visibility=PRIVATE -> owner_id NOT NULL; GROUP může mít NULL
--   6. Index na (visibility, training_date) pro rychlé dashboard queries
-- ============================================================================

DROP TABLE IF EXISTS group_lesson_plan;

ALTER TABLE training
    ADD COLUMN visibility       VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN created_by_id    BIGINT      REFERENCES account(id) ON DELETE SET NULL;

-- Owner_id už nemá být NOT NULL — GROUP tréninky nemají vlastníka
ALTER TABLE training ALTER COLUMN owner_id DROP NOT NULL;

-- Vynucení invariantu: PRIVATE training musí mít owner_id; GROUP může mít NULL
ALTER TABLE training ADD CONSTRAINT training_visibility_owner_check CHECK (
    (visibility = 'PRIVATE' AND owner_id IS NOT NULL) OR
    (visibility = 'GROUP')
);

ALTER TABLE training ADD CONSTRAINT training_visibility_check CHECK (
    visibility IN ('PRIVATE', 'GROUP')
);

-- Pro dashboard: rychle najít group tréninky pro daný den
CREATE INDEX idx_training_group_date ON training (training_date)
    WHERE visibility = 'GROUP';
