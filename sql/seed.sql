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
