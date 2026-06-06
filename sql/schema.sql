CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'STUDENT')),
    full_name VARCHAR(120) NOT NULL,
    department VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('TECHNICAL', 'CULTURAL', 'SPORTS', 'WORKSHOP', 'SEMINAR')),
    venue VARCHAR(160) NOT NULL,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    registered_count INTEGER NOT NULL DEFAULT 0 CHECK (registered_count >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'UPCOMING' CHECK (status IN ('UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED')),
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT events_time_order CHECK (end_time > start_time),
    CONSTRAINT events_capacity_count CHECK (registered_count <= capacity)
);

CREATE TABLE IF NOT EXISTS registrations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE RESTRICT,
    registration_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unique_reg_id VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS certificates (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL UNIQUE REFERENCES registrations(id) ON DELETE CASCADE,
    certificate_path TEXT NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_id BIGINT REFERENCES events(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_events_category ON events(category);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_registrations_user_id ON registrations(user_id);
CREATE INDEX IF NOT EXISTS idx_registrations_event_id ON registrations(event_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_registrations_unique_confirmed
    ON registrations(user_id, event_id)
    WHERE status = 'CONFIRMED';
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_event_reminder_unique
    ON notifications(user_id, event_id, title)
    WHERE title = 'Event Reminder';

DO $$
DECLARE
    event_fk_name TEXT;
BEGIN
    SELECT constraint_name
    INTO event_fk_name
    FROM information_schema.referential_constraints
    WHERE constraint_schema = current_schema()
      AND constraint_name IN (
          SELECT constraint_name
          FROM information_schema.key_column_usage
          WHERE table_schema = current_schema()
            AND table_name = 'registrations'
            AND column_name = 'event_id'
      )
    LIMIT 1;

    IF event_fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE registrations DROP CONSTRAINT %I', event_fk_name);
    END IF;

    ALTER TABLE registrations
        ADD CONSTRAINT registrations_event_id_fkey
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE RESTRICT;
END
$$;

INSERT INTO users (username, email, password_hash, role, full_name, department)
VALUES (
    'admin',
    'admin@college.edu',
    '$2a$10$U31f6jtCyDqTdaYEof1qguXHSE7AMLHPhbgzhpMWoWOn6W4qOFFhq',
    'ADMIN',
    'Portal Administrator',
    'Administration'
)
ON CONFLICT (username) DO NOTHING;
