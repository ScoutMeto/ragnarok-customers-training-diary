-- ScoutMeto kolo 6: zapomenuté heslo. Uživatel si nechá poslat 6místný kód na email,
-- zadá ho + nové heslo. Kód má TTL (stejně jako email confirmation). Oddělené sloupce
-- od email_confirmation_code, aby se navzájem nepřepisovaly.

ALTER TABLE account
    ADD COLUMN password_reset_code        VARCHAR(8),
    ADD COLUMN password_reset_expires_at  TIMESTAMP;
