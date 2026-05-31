-- Phase 9 (A13): difficulty LIGHT/MEDIUM/HARD → 9 konkrétních zaměření ve 3 úrovních.
--
-- Stará data (3 hodnoty) mapujeme na reprezentativní zaměření v dané úrovni:
--   LIGHT  → LEHKY_TRENINK
--   MEDIUM → SILOVE_KONDICNI
--   HARD   → ROZVOJ_MAX_SILY
-- Sloupec training.difficulty je VARCHAR bez CHECK constraintu, takže stačí UPDATE.

UPDATE training SET difficulty = CASE difficulty
    WHEN 'LIGHT'  THEN 'LEHKY_TRENINK'
    WHEN 'MEDIUM' THEN 'SILOVE_KONDICNI'
    WHEN 'HARD'   THEN 'ROZVOJ_MAX_SILY'
    ELSE difficulty
END
WHERE difficulty IS NOT NULL;
