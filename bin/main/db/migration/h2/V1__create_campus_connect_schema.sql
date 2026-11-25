CREATE TABLE roles (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE users (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE students (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    student_number VARCHAR(40) NOT NULL UNIQUE,
    department_id VARCHAR(36),
    academic_year SMALLINT
);

CREATE TABLE event_categories (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE events (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id VARCHAR(36) NOT NULL REFERENCES event_categories(id),
    organizer_id VARCHAR(36) NOT NULL REFERENCES users(id),
    venue VARCHAR(200) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    registration_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    capacity INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE event_registrations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    student_id VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reminder_sent_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_event_student UNIQUE (event_id, student_id)
);

CREATE INDEX idx_events_status_starts_at ON events(status, starts_at);
CREATE INDEX idx_event_registrations_student ON event_registrations(student_id, registered_at);
CREATE INDEX idx_event_registrations_event_status ON event_registrations(event_id, status);

INSERT INTO roles (id, name) VALUES
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'ROLE_STUDENT'),
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'ROLE_ORGANIZER'),
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'ROLE_ADMIN');
INSERT INTO event_categories (id, name) VALUES
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'Seminar'),
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'Workshop'),
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'Hackathon'),
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'Conference'),
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'Career'),
    (CAST(RANDOM_UUID() AS VARCHAR(36)), 'Social');
