-- Phase 19b (ScoutMeto): rework notifikačních preferencí.
-- Přidáváme 2 nové: potvrzení o vytvoření tréninku + upozornění na deaktivaci účtu.
-- Staré notif_welcome a notif_group_training_reminder zůstávají ve schématu (kvůli
-- kompatibilitě), ale v UI je už nenabízíme a maily se nerozesílají.

ALTER TABLE account
    ADD COLUMN notif_training_created    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notif_account_deactivated BOOLEAN NOT NULL DEFAULT TRUE;
