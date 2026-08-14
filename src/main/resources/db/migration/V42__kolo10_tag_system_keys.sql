-- ScoutMeto kolo 10: systémové tagy — stabilní klíč + počeštění + úprava sady.
--
-- Proč system_key: UI i budoucí statistiky rozhodují podle konkrétních tagů
-- (Nošení → metry/sekundy, Izometrie → jen sekundy, Repetitivní provedení →
-- série/opakování/kg). Do kola 9 se to rozlišovalo podle NÁZVU tagu, takže
-- jakékoliv přejmenování logiku tiše rozbilo. Od teď je pravdou klíč a název
-- je jen popisek, který smí Meto libovolně měnit.

-- 1) Sloupec + unikátnost (jen pro neprázdné klíče — custom tagy mají NULL)
ALTER TABLE training_tag ADD COLUMN system_key VARCHAR(32);
CREATE UNIQUE INDEX uq_training_tag_system_key
    ON training_tag (system_key) WHERE system_key IS NOT NULL;

-- 2) Doplnění klíčů ke stávajícím systémovým tagům (podle aktuálních názvů po V41)
UPDATE training_tag SET system_key = 'KETTLEBELL'        WHERE is_system AND name = 'Kettlebell';
UPDATE training_tag SET system_key = 'BODYWEIGHT'        WHERE is_system AND name = 'Vlastní váha';
UPDATE training_tag SET system_key = 'CARDIO'            WHERE is_system AND name = 'Kardio';
UPDATE training_tag SET system_key = 'OS_RESETS'         WHERE is_system AND name = 'OS Resets';
UPDATE training_tag SET system_key = 'MOBILITY'          WHERE is_system AND name = 'Mobilita';
UPDATE training_tag SET system_key = 'STRENGTH'          WHERE is_system AND name = 'Síla';
UPDATE training_tag SET system_key = 'STRENGTH_ENDURANCE' WHERE is_system AND name = 'Kondice';
UPDATE training_tag SET system_key = 'ISOLATION'         WHERE is_system AND name = 'Isolation';
UPDATE training_tag SET system_key = 'STRETCHING'        WHERE is_system AND name = 'Stretching';
UPDATE training_tag SET system_key = 'BARBELL'           WHERE is_system AND name = 'Barbell';
UPDATE training_tag SET system_key = 'OTHERS'            WHERE is_system AND name = 'Others';
UPDATE training_tag SET system_key = 'CORE'              WHERE is_system AND name = 'Střed těla';
UPDATE training_tag SET system_key = 'FULL_BODY'         WHERE is_system AND name = 'Celé tělo';
UPDATE training_tag SET system_key = 'UPPER_BODY'        WHERE is_system AND name = 'Horní část těla';
UPDATE training_tag SET system_key = 'LOWER_BODY'        WHERE is_system AND name = 'Dolní část těla';
UPDATE training_tag SET system_key = 'UNILATERAL'        WHERE is_system AND name = 'Unilaterální';
UPDATE training_tag SET system_key = 'UNILATERAL_LEFT'   WHERE is_system AND name = 'Unilaterální levá';
UPDATE training_tag SET system_key = 'UNILATERAL_RIGHT'  WHERE is_system AND name = 'Unilaterální pravá';
UPDATE training_tag SET system_key = 'BILATERAL'         WHERE is_system AND name = 'Bilaterální';
UPDATE training_tag SET system_key = 'CARRY'             WHERE is_system AND name = 'Carry';
UPDATE training_tag SET system_key = 'ISOMETRY'          WHERE is_system AND name = 'Isometrie';

-- 3) Počeštění názvů (klíč zůstává, takže se nic v UI logice nerozbije)
UPDATE training_tag SET name = 'Nošení'             WHERE system_key = 'CARRY';
UPDATE training_tag SET name = 'Izolované cvičení'  WHERE system_key = 'ISOLATION';
UPDATE training_tag SET name = 'Jiné'               WHERE system_key = 'OTHERS';
UPDATE training_tag SET name = 'Strečink'           WHERE system_key = 'STRETCHING';
UPDATE training_tag SET name = 'Olympijská osa'     WHERE system_key = 'BARBELL';
UPDATE training_tag SET name = 'Silová vytrvalost'  WHERE system_key = 'STRENGTH_ENDURANCE';

-- 4) Zrušit tag „Síla a kondice" (kolo 10). Linky na tréninky/cviky/kroky odpadnou
--    kaskádou (všechny tři link tabulky mají ON DELETE CASCADE na tag_id).
DELETE FROM training_tag WHERE is_system AND name = 'Síla a kondice';

-- 5) Nový systémový tag: „Repetitivní provedení" — protipól Nošení/Izometrie.
--    Označuje cvik, který se počítá na opakování (a tedy smí do statistik
--    série / opakování / nazvedané kg).
INSERT INTO training_tag (name, color, is_system, owner_id, system_key)
VALUES ('Repetitivní provedení', '#00838F', TRUE, NULL, 'REPETITIVE');
