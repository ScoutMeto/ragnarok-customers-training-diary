-- ScoutMeto kolo 7: „Uložit, zatím nezveřejňovat" pro skupinové tréninky (klasické i textové).
--  - training.published: nepublikovaný GROUP trénink klienti nevidí (admin ho vidí zašedlý).
--  - text_plan.published + published_at: textová skupinová nabídka; datum publikace určuje
--    týden, ve kterém ji uživatelé vidí (pondělní reset — nabídka minulého týdne zmizí).

ALTER TABLE training
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE text_plan
    ADD COLUMN published    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN published_at DATE;

-- Backfill: existující skupinové nabídky považujeme za publikované dnem vytvoření
UPDATE text_plan SET published_at = created_at::date WHERE group_offer = TRUE;
