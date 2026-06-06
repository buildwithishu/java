INSERT INTO users (username, email, password_hash, role, full_name, department)
VALUES
    ('student1', 'student1@college.edu', '$2a$10$U31f6jtCyDqTdaYEof1qguXHSE7AMLHPhbgzhpMWoWOn6W4qOFFhq', 'STUDENT', 'Aarav Sharma', 'Computer Science'),
    ('student2', 'student2@college.edu', '$2a$10$U31f6jtCyDqTdaYEof1qguXHSE7AMLHPhbgzhpMWoWOn6W4qOFFhq', 'STUDENT', 'Ananya Verma', 'Electronics')
ON CONFLICT (username) DO NOTHING;

INSERT INTO events (
    title,
    description,
    category,
    venue,
    event_date,
    start_time,
    end_time,
    capacity,
    status,
    created_by
)
SELECT event_data.title,
       event_data.description,
       event_data.category,
       event_data.venue,
       event_data.event_date,
       event_data.start_time,
       event_data.end_time,
       event_data.capacity,
       event_data.status,
       admin_user.id
FROM (
    VALUES
        ('Code Sprint 2026', 'A competitive programming event for all departments.', 'TECHNICAL', 'Lab Block A', DATE '2026-07-10', TIME '10:00', TIME '13:00', 80, 'UPCOMING'),
        ('Cultural Night', 'Music, dance, and theatre performances by student clubs.', 'CULTURAL', 'Main Auditorium', DATE '2026-07-18', TIME '17:00', TIME '21:00', 300, 'UPCOMING'),
        ('Inter-Department Football', 'Knockout football tournament for department teams.', 'SPORTS', 'Sports Ground', DATE '2026-07-25', TIME '08:00', TIME '12:00', 120, 'UPCOMING'),
        ('Resume Workshop', 'Hands-on session for building resumes and LinkedIn profiles.', 'WORKSHOP', 'Seminar Hall 2', DATE '2026-08-02', TIME '11:00', TIME '14:00', 60, 'UPCOMING'),
        ('AI Seminar', 'Guest lecture on practical AI systems and responsible use.', 'SEMINAR', 'Conference Room', DATE '2026-08-12', TIME '15:00', TIME '17:00', 100, 'UPCOMING')
) AS event_data(title, description, category, venue, event_date, start_time, end_time, capacity, status)
CROSS JOIN users admin_user
WHERE admin_user.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM events existing
      WHERE existing.title = event_data.title
        AND existing.event_date = event_data.event_date
  );

INSERT INTO notifications (user_id, title, message)
SELECT id,
       'Welcome to Event Portal',
       'Browse upcoming college events and register from your dashboard.'
FROM users
WHERE role = 'STUDENT'
  AND NOT EXISTS (
      SELECT 1
      FROM notifications
      WHERE notifications.user_id = users.id
        AND notifications.title = 'Welcome to Event Portal'
  );

INSERT INTO registrations (user_id, event_id, unique_reg_id, status)
SELECT student.id,
       event.id,
       seed_registration.unique_reg_id,
       'CONFIRMED'
FROM (
    VALUES
        ('student1', 'Code Sprint 2026', DATE '2026-07-10', 'SEED-CODE-SPRINT-STUDENT1'),
        ('student2', 'Cultural Night', DATE '2026-07-18', 'SEED-CULTURAL-NIGHT-STUDENT2')
) AS seed_registration(username, event_title, event_date, unique_reg_id)
JOIN users student ON student.username = seed_registration.username
JOIN events event
  ON event.title = seed_registration.event_title
 AND event.event_date = seed_registration.event_date
WHERE NOT EXISTS (
    SELECT 1
    FROM registrations existing
    WHERE existing.user_id = student.id
      AND existing.event_id = event.id
      AND existing.status = 'CONFIRMED'
);

UPDATE events event
SET registered_count = (
        SELECT COUNT(*)
        FROM registrations registration
        WHERE registration.event_id = event.id
          AND registration.status = 'CONFIRMED'
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM registrations registration
    WHERE registration.event_id = event.id
);

INSERT INTO notifications (user_id, title, message, event_id)
SELECT registration.user_id,
       'Registration Confirmed',
       'Your sample registration for ' || event.title || ' is confirmed.',
       event.id
FROM registrations registration
JOIN events event ON event.id = registration.event_id
WHERE registration.unique_reg_id LIKE 'SEED-%'
  AND NOT EXISTS (
      SELECT 1
      FROM notifications existing
      WHERE existing.user_id = registration.user_id
        AND existing.event_id = event.id
        AND existing.title = 'Registration Confirmed'
  );
