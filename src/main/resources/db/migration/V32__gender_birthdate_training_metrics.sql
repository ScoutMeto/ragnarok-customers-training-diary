-- ScoutMeto kolo 5:
--  1) Profil uživatele: pohlaví (Muž/Žena) + datum narození (pro výpočet věku a MTF).
--  2) Trénink: zdravotní/kondiční metriky (bodyweight, tepové frekvence, spánek)
--     + záznam ženského cyklu (den cyklu + fáze). Vše nullable — nepovinné.

-- --- Profil účtu ---
ALTER TABLE account
    ADD COLUMN gender     VARCHAR(8),   -- MALE | FEMALE | NULL (neuvedeno)
    ADD COLUMN birth_date DATE;

-- --- Tréninkové metriky + cyklus ---
ALTER TABLE training
    ADD COLUMN bodyweight_kg     NUMERIC(5,2),  -- aktuální tělesná hmotnost
    ADD COLUMN resting_hr_bpm    SMALLINT,      -- klidová tepová frekvence (ráno)
    ADD COLUMN sleep_quality     SMALLINT,      -- kvalita spánku 0-100
    ADD COLUMN sleep_quality_rpe SMALLINT,      -- subjektivní hodnocení spánku 1-10
    ADD COLUMN avg_hr_bpm        SMALLINT,      -- průměrná TF během tréninku
    ADD COLUMN max_hr_bpm        SMALLINT,      -- nejvyšší TF během tréninku
    ADD COLUMN cycle_day         SMALLINT,      -- den ženského cyklu (1-35)
    ADD COLUMN cycle_phase       VARCHAR(16);   -- MENSTRUAL | FOLLICULAR | OVULATORY | LUTEAL
