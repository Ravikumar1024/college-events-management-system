ALTER TABLE events ADD COLUMN organizer_reminder_sent_at TIMESTAMP WITH TIME ZONE;

UPDATE event_registrations SET reminder_sent_at = NULL;
