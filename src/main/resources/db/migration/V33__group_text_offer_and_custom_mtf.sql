-- ScoutMeto kolo 6:
--  1) Skupinový textový trénink: text_plan dostane příznak group_offer.
--     group_offer=true (+ is_template=true, owner=null) = nabídka pro všechny uživatele;
--     uživatel si ji „přidá" → vznikne jeho editovatelná kopie (group_offer=false, owner=user).
--  2) Vlastní (změřená) maximální tepová frekvence — přebíjí vypočtenou pro doporučené zóny.

ALTER TABLE text_plan
    ADD COLUMN group_offer BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE account
    ADD COLUMN custom_max_hr SMALLINT;
