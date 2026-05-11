-- ============================================================
-- UDJCS — Database Initialization
-- Module 01: Organization Settings
-- Module 02: Supportive Organization
-- ============================================================

CREATE TABLE IF NOT EXISTS organizations (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    registration_number VARCHAR(50),
    trust_type       VARCHAR(100),
    address          VARCHAR(255),
    city             VARCHAR(100),
    state            VARCHAR(100),
    pincode          VARCHAR(10),
    phone            VARCHAR(20),
    email            VARCHAR(100),
    website          VARCHAR(150),
    established_year INTEGER,
    description      TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

-- Module 02: Supportive Organization
CREATE TABLE IF NOT EXISTS supportive_organizations (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    organization_type   VARCHAR(100),
    contact_person      VARCHAR(100),
    contact_phone       VARCHAR(20),
    contact_email       VARCHAR(100),
    address             VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(100),
    website             VARCHAR(150),
    description         TEXT,
    logo_path           VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);
