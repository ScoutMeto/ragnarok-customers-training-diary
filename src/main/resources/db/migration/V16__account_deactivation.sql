-- Phase 10 (E1): deaktivace účtu (read-only mód pro neplatiče).
--
-- deactivated_at NULL = aktivní účet (default).
-- deactivated_at má hodnotu = účet je read-only: klient se může přihlásit,
-- vidí svoje tréninky a statistiky, ALE nemůže přidávat/upravovat tréninky
-- a nevidí nabídky skupinových lekcí. Spravuje libovolný admin (toggle).

ALTER TABLE account
    ADD COLUMN deactivated_at TIMESTAMP;

COMMENT ON COLUMN account.deactivated_at IS
    'Phase 10: NULL = aktivní. Hodnota = read-only mód (neplatič). Liší se od deleted_at (soft delete).';
