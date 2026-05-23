-- ============================================================================
-- V1: Account table (unified user/admin model)
--
-- Role rozhoduje o oprávněních:
--   USER  = klient (loguje vlastní tréninky, vidí GroupLessonPlan ±1 týden)
--   ADMIN = trenér (přístup ke všem klientům, spravuje katalog/plány)
-- ============================================================================

CREATE TABLE account (
    id                          BIGSERIAL    PRIMARY KEY,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password_hash               VARCHAR(255) NOT NULL,
    role                        VARCHAR(16)  NOT NULL,
    nickname                    VARCHAR(64)  NOT NULL,
    first_name                  VARCHAR(64)  NOT NULL,
    last_name                   VARCHAR(64)  NOT NULL,
    phone                       VARCHAR(32),
    email_notifications_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT account_role_check CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_account_email ON account (email);
CREATE INDEX idx_account_role  ON account (role);
