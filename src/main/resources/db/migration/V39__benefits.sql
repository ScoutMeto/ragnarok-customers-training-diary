-- ScoutMeto kolo 7: sekce „Výhody". Společná tabulka výhod pro všechny USER (edituje
-- admin v /admin/overview). Řádek = název + popis + nápověda (text NEBO tlačítko).
-- Tlačítko: uživatel napíše volitelný text → odešle se e-mail na adresu nastavenou
-- adminem (předmět + text uživatele + skrytý přednastavený text + údaje uživatele).
-- Viditelnost tabulky lze vypnout per-user (account.benefits_visible).

CREATE TABLE benefit_item (
    id                 BIGSERIAL PRIMARY KEY,
    position           INT NOT NULL DEFAULT 0,        -- pořadí řádků
    name               VARCHAR(128) NOT NULL,          -- 1. sloupec: název výhody
    description        TEXT,                           -- 2. sloupec: popis
    help_type          VARCHAR(8) NOT NULL DEFAULT 'TEXT',  -- TEXT | BUTTON
    help_text          TEXT,                           -- 3. sloupec při TEXT
    button_label       VARCHAR(64),                    -- popisek tlačítka (default „Využít výhodu")
    button_email       VARCHAR(255),                   -- kam se požadavek posílá
    button_subject     VARCHAR(255),                   -- předmět mailu
    button_preset_text TEXT,                           -- skrytý přednastavený text (ověření z aplikace)
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE account
    ADD COLUMN benefits_visible BOOLEAN NOT NULL DEFAULT TRUE;
