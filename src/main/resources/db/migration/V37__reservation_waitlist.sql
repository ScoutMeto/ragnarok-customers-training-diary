-- ScoutMeto kolo 7: Náhradník. Na plnou lekci se lze přihlásit jako náhradník
-- (max tolik náhradníků, kolik je kapacita lekce). Když se místo uvolní, první
-- náhradník v pořadí dostane automaticky rezervaci + e-mail. Waitlist žije celý
-- v diary (nulový zásah do rezervačního systému) — promoce běží okamžitě při
-- zrušení přes diary + kontrolním jobem (zachytí i uvolnění adminem v rez. systému).

CREATE TABLE reservation_waitlist (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    ext_training_id BIGINT NOT NULL,          -- id lekce v rezervačním systému
    training_title  VARCHAR(255),
    training_start  TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),   -- určuje pořadí náhradníků
    status          VARCHAR(16) NOT NULL DEFAULT 'WAITING',  -- WAITING | PROMOTED
    promoted_at     TIMESTAMP,
    CONSTRAINT uq_waitlist_account_training UNIQUE (account_id, ext_training_id)
);

CREATE INDEX idx_waitlist_training_status ON reservation_waitlist (ext_training_id, status);
