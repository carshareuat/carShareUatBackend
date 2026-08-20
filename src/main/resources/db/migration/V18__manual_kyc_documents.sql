ALTER TABLE users
    DROP COLUMN IF EXISTS didit_session_id,
    DROP COLUMN IF EXISTS didit_user_id,
    DROP COLUMN IF EXISTS verification_status,
    DROP COLUMN IF EXISTS document_type,
    DROP COLUMN IF EXISTS id_number_hash;

DROP TABLE IF EXISTS digilocker_kyc_logs;

ALTER TABLE kyc_documents
    MODIFY COLUMN owner_id CHAR(36) NULL,
    ADD COLUMN IF NOT EXISTS user_id CHAR(36) NULL;

ALTER TABLE kyc_documents
    ADD CONSTRAINT fk_kyc_documents_user FOREIGN KEY (user_id) REFERENCES users(id);
