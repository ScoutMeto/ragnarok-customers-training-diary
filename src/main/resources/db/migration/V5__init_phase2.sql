-- ============================================================================
-- V5: Phase 2 — Trenér + GroupLessonPlan + komentáře + soft delete
--
-- Změny:
--   1. account: přidat `deleted_at TIMESTAMP NULL` pro soft delete (anonymizace)
--   2. group_lesson_plan: informativní zápis lekce (datum, čas, název, trenér).
--      Žádná kapacita, žádné recurring, žádný link na rezervace — to je
--      samostatná aplikace (Phase 7).
--   3. training_comment: komentář k tréninku (autor = klient nebo admin).
-- ============================================================================


-- =============================================================================
-- 1. Soft delete na account
-- =============================================================================
ALTER TABLE account ADD COLUMN deleted_at TIMESTAMP;
CREATE INDEX idx_account_active ON account (deleted_at) WHERE deleted_at IS NULL;


-- =============================================================================
-- 2. Skupinové lekce (informativní pohled, ±1 týden pro klienta)
-- =============================================================================
CREATE TABLE group_lesson_plan (
    id              BIGSERIAL    PRIMARY KEY,
    lesson_date     DATE         NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME,
    lesson_name     VARCHAR(128) NOT NULL,
    coach_id        BIGINT       NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    description     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lesson_date ON group_lesson_plan (lesson_date);


-- =============================================================================
-- 3. Komentáře k tréninku
-- =============================================================================
CREATE TABLE training_comment (
    id           BIGSERIAL    PRIMARY KEY,
    training_id  BIGINT       NOT NULL REFERENCES training(id)  ON DELETE CASCADE,
    author_id    BIGINT       NOT NULL REFERENCES account(id)   ON DELETE CASCADE,
    text         TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comment_training ON training_comment (training_id, created_at DESC);
