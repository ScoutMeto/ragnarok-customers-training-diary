-- Phase 6: email potvrzení při registraci + notifikační preference.

-- =====================================================================
-- 1) Email confirmation pole
-- =====================================================================
ALTER TABLE account
    ADD COLUMN email_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_confirmation_code VARCHAR(8),
    ADD COLUMN email_confirmation_code_sent_at TIMESTAMP,
    ADD COLUMN email_confirmation_code_expires_at TIMESTAMP;

CREATE INDEX idx_account_email_confirmation_code
    ON account(email_confirmation_code)
    WHERE email_confirmation_code IS NOT NULL;

-- =====================================================================
-- 2) Notifikační preference (per-user, default zapnuté)
-- =====================================================================
ALTER TABLE account
    ADD COLUMN notif_group_training_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notif_new_plan_assigned       BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notif_new_comment             BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notif_welcome                 BOOLEAN NOT NULL DEFAULT TRUE;

-- =====================================================================
-- 3) Auto-confirm všechny existující účty (admin@admin.cz + cokoliv co je v DB)
-- =====================================================================
UPDATE account
SET email_confirmed = TRUE
WHERE email_confirmed = FALSE;

COMMENT ON COLUMN account.email_confirmed IS
    'true = user prošel email confirmation po registraci; existující účty v V13 auto-true.';
COMMENT ON COLUMN account.email_confirmation_code IS
    '6místný kód odeslaný na email; mazaný po úspěšném potvrzení.';
COMMENT ON COLUMN account.notif_group_training_reminder IS
    'Cron 18:00: pošli mail klientům, kteří mají zítra skupinový trénink.';
COMMENT ON COLUMN account.notif_new_plan_assigned IS
    'Trenér přiřadí šablonu nebo coach plan → klient dostane mail.';
COMMENT ON COLUMN account.notif_new_comment IS
    'Někdo přidá komentář k tvému (nebo group) tréninku → mail.';
COMMENT ON COLUMN account.notif_welcome IS
    'Uvítací mail po potvrzení emailu.';
