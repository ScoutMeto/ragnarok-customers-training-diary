-- ScoutMeto kolo 10: CARDIO = dlouhé pomalé kardio (long-slow cardio).
--
-- Model je záměrně obecný: neváže se na běh/kolo/plavání (aktivita se vybírá
-- z katalogu cviků jako u všech ostatních typů) a nevyžaduje jednu konkrétní
-- jednotku výkonu. Jediný povinný údaj je doba trvání — všechny objemové
-- metriky jsou volitelné a dají se kombinovat.

CREATE TABLE cardio_config (
    training_exercise_id BIGINT  PRIMARY KEY REFERENCES training_exercise(id) ON DELETE CASCADE,

    -- Celkový čas od zahájení do ukončení aktivity VČETNĚ přestávek.
    elapsed_seconds      INT     NOT NULL,
    -- Čistý čas aktivity bez přestávek. Dopočítává se (elapsed − suma pauz),
    -- ale uživatel ho smí přepsat podle údaje z hodinek.
    active_seconds       INT,

    -- Volitelné objemové metriky — žádná není povinná, lze je kombinovat.
    distance_m           INT,          -- interně vždy v metrech, UI zobrazuje m/km
    repetitions          INT,
    steps                INT,
    elevation_gain_m     INT,

    -- Odvozené hodnoty. Aplikace je dopočítá z activeTime, uživatel je smí přepsat
    -- (např. přesnější číslo přímo ze sporttesteru).
    avg_speed_kmh        NUMERIC(6, 2),
    avg_pace_s_per_km    INT,

    notes                TEXT,
    CONSTRAINT cardio_elapsed_check CHECK (elapsed_seconds > 0)
);

-- Přestávky nejsou intervaly — jsou to události uvnitř jednoho cardio záznamu.
CREATE TABLE cardio_pause (
    id                      BIGSERIAL PRIMARY KEY,
    cardio_config_id        BIGINT    NOT NULL REFERENCES cardio_config(training_exercise_id) ON DELETE CASCADE,
    order_index             INT       NOT NULL,
    start_from_begin_s      INT       NOT NULL,   -- čas od začátku aktivity, kdy pauza začala
    duration_seconds        INT       NOT NULL,
    active_pause            BOOLEAN   NOT NULL DEFAULT FALSE,  -- aktivní (chůze, protahování) × pasivní
    distance_at_pause_m     INT,
    repetitions_at_pause    INT,
    steps_at_pause          INT,
    note                    VARCHAR(255)
);
CREATE INDEX idx_cardio_pause ON cardio_pause (cardio_config_id, order_index);
