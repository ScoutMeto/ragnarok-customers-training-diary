-- ScoutMeto kolo 6 (review fix): text_plan.source_template_id mělo mít ON DELETE SET NULL,
-- stejně jako training.source_template_id (V12). Bez toho smazání skupinové nabídky / šablony,
-- na kterou odkazuje aspoň jedna uživatelská kopie, padlo na FK violation (HTTP 500) — a UI
-- přitom slibuje „Kopie u uživatelů zůstanou". Po této změně se kopiím jen vynuluje odkaz.

ALTER TABLE text_plan DROP CONSTRAINT text_plan_source_template_id_fkey;
ALTER TABLE text_plan ADD CONSTRAINT text_plan_source_template_id_fkey
    FOREIGN KEY (source_template_id) REFERENCES text_plan(id) ON DELETE SET NULL;
