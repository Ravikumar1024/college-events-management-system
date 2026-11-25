ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN normalized_email VARCHAR(180);
UPDATE users SET normalized_email = LOWER(TRIM(email));
ALTER TABLE users ALTER COLUMN normalized_email SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_normalized_email UNIQUE (normalized_email);

ALTER TABLE students ADD COLUMN normalized_student_number VARCHAR(40);
UPDATE students SET normalized_student_number = UPPER(TRIM(student_number));
ALTER TABLE students ALTER COLUMN normalized_student_number SET NOT NULL;
ALTER TABLE students ADD CONSTRAINT uq_students_normalized_student_number UNIQUE (normalized_student_number);

CREATE TABLE email_verification_tokens (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    otp_hash VARCHAR(100) NOT NULL,
    activation_token_hash VARCHAR(64) NOT NULL,
    otp_expires_at TIMESTAMPTZ NOT NULL,
    activation_expires_at TIMESTAMPTZ NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    last_sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE event_invitations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    student_id VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    invited_by_user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    CONSTRAINT uq_event_invitation UNIQUE (event_id, student_id)
);

CREATE TABLE notifications (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    recipient_user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id VARCHAR(36) REFERENCES events(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    reference_id VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_event_invitations_student_status ON event_invitations(student_id, status, created_at);
CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_user_id, created_at);
