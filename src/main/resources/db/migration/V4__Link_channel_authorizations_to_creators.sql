ALTER TABLE channel_authorizations
    ADD COLUMN creator_account_id INTEGER REFERENCES creator_accounts(id) ON DELETE SET NULL;

CREATE INDEX idx_channel_authorizations_creator
    ON channel_authorizations(creator_account_id);
