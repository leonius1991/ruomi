ALTER TABLE advertisements
    ADD COLUMN external_source VARCHAR(50) NULL,
    ADD COLUMN external_id VARCHAR(64) NULL;

CREATE UNIQUE INDEX idx_advertisements_external
    ON advertisements (external_source, external_id);
