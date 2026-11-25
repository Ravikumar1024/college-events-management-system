# Offbeat

Offbeat brings campus seminars, workshops, hackathons and other events into one place. Students can discover and save events in their browser, register, manage their plans and receive email updates. Organizers can publish events, check in attendees and download participation reports.

## Run locally

Requirements: Java 21. Local testing uses an in-memory H2 database and does not require PostgreSQL or Docker.

1. Start the app. H2 and Flyway create the in-memory database, schema, and default event categories on startup:

   ```powershell
   .\gradlew.bat bootRun
   ```

   Open [http://localhost:8080/events](http://localhost:8080/events). Spring Boot serves both the frontend and API on port `8080`; no separate frontend server is required. The browser routes are `/events`, `/register`, `/verify-email`, `/login`, `/dashboard`, `/my-events`, `/organizer`, and `/admin/students`. The authenticated dashboard adapts to student, organizer, or admin roles.

   The in-memory database is reset whenever the app restarts. Use the Spring Boot address above for clean browser routes. If you open individual `.html` files through VS Code Live Server, use the `.html` URLs because that server does not know the Spring MVC routes.

   To inspect tables and run SQL, open [http://localhost:8080/h2-console](http://localhost:8080/h2-console). Use JDBC URL `jdbc:h2:mem:campus_connect;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`, username `sa`, and leave the password blank. The console is restricted to local access; disable it with `H2_CONSOLE_ENABLED=false` outside local development.

Create a student account from the site. New student accounts must be verified with the emailed one-time code or activation link before sign-in. Email notifications are disabled by default for local testing. The application is branded Offbeat. Entity IDs are UUID strings and should be treated as opaque values. The initial Flyway schema creates UUID columns directly; use a new database rather than an existing database with numeric-ID migration history. Existing records are not migrated.

Emails use an Offbeat HTML layout with a plain-text alternative. A student seat request waits for the event organizer; approval confirms the seat and sends the confirmation email. Invitations appear in the student's notifications and can be accepted or declined; accepting reserves a seat only if capacity remains. Cancelling a confirmed seat sends an email. Once per published event, the reminder worker scans every minute and, when an event is within 30 minutes, emails each currently registered student and sends the organizer one message with the registered attendee list. Reminder emails are sent only when notifications are enabled and SMTP is configured.

## Roles and account relationships

Each `users` row can have multiple role links through `user_roles` (for example, one user can be both `ROLE_ADMIN` and `ROLE_STUDENT`). Student registration creates both a `ROLE_STUDENT` role link and a `students` profile row. The profile stores the student number and is required for student registration features.

- **Student:** discovers events, requests seats, responds to invitations, and views/cancels their own registrations.
- **Organizer:** publishes events; approves or rejects seat requests; invites students associated with their events; and manages attendance/reports only for events they organize.
- **Admin:** broad management access, including creating events and managing/viewing any organizer event. The student directory can deactivate student accounts while preserving event history. Admin does not automatically mean student; add `ROLE_STUDENT` and a student profile only when the admin also uses student features.

Public registration only creates student accounts. Organizer and admin access must be assigned by an administrator. To grant organizer access to an existing account (SQL works with the included H2 and PostgreSQL schemas):

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE LOWER(u.email) = LOWER('organizer@college.edu')
  AND r.name = 'ROLE_ORGANIZER'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
```

Log out and back in after assigning a role so the page navigation reflects the updated account. Organizers can then publish events from the **Host desk**, review and approve seat requests, invite eligible students, mark attendance, and download a CSV participation report.

If an authenticated API request returns `401 Unauthorized`, Offbeat clears the expired session and redirects to login. Permission errors (`403`) and other API failures do not log the user out.

## Email configuration

To test email locally, start Mailpit with `docker compose up -d mailpit`, then set `$env:NOTIFICATION_EMAIL_ENABLED = "true"` before starting the API; captured mail is available at [http://localhost:8025](http://localhost:8025). To use Brevo SMTP instead, set the SMTP login and a freshly rotated SMTP key as environment variables before starting the API; credentials are deliberately not stored in `application.yml`:

| Variable | Default | Description |
| --- | --- | --- |
| `BREVO_SMTP_LOGIN` | empty | Brevo SMTP login |
| `BREVO_SMTP_KEY` | empty | Brevo SMTP key; keep private and rotate if exposed |
| `MAIL_HOST` | `localhost` | SMTP server (use `smtp-relay.brevo.com` for Brevo) |
| `MAIL_PORT` | `1025` | SMTP port (use `587` for Brevo) |
| `MAIL_SMTP_AUTH` | `false` | Enable SMTP authentication |
| `MAIL_SMTP_STARTTLS` | `false` | Enable STARTTLS |
| `MAIL_FROM` | `offbeat@localhost` | Sender address (use a verified address for Brevo) |
| `NOTIFICATION_EMAIL_ENABLED` | `false` | Enable or disable notification delivery |
| `FRONTEND_URL` | `http://localhost:8080` | Link target in email messages |
| `FRONTEND_ORIGIN` | `http://localhost:5500` | Allowed browser origin |

To use PostgreSQL instead of the default H2 database, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER=org.postgresql.Driver` before starting the API.

In PowerShell, provide the rotated login/key only in the environment of the process you start:

```powershell
$env:BREVO_SMTP_LOGIN = "your-brevo-smtp-login"
$env:BREVO_SMTP_KEY = "your-new-brevo-smtp-key"
.\gradlew.bat bootRun
```

The configured sender address must be verified and authorized in your Brevo account. Do not enable SMTP debug logging in production.

For local Mailpit testing, the defaults above are already configured. For Brevo, also set `$env:NOTIFICATION_EMAIL_ENABLED = "true"`, `$env:MAIL_HOST = "smtp-relay.brevo.com"`, `$env:MAIL_PORT = "587"`, `$env:MAIL_SMTP_AUTH = "true"`, and `$env:MAIL_SMTP_STARTTLS = "true"` alongside your login and key.

If SMTP delivery fails during registration or cancellation, the API reports an email delivery error rather than showing a false success.

## Main API routes

- `GET /api/events`, `GET /api/events/categories` — published events and categories.
- `POST /api/auth/register`, `POST /api/auth/login` — create a student account and sign in; registration is followed by email verification.
- `POST /api/auth/verify-otp`, `POST /api/auth/verify-link`, `POST /api/auth/resend-verification` — verify a new account by OTP or activation link and resend verification.
- `POST /api/events/{eventId}/registrations`, `GET /api/students/me/registrations`, `DELETE /api/registrations/{id}` — request/cancel a seat and view personal registrations.
- `GET /api/students/me/invitations`, `POST /api/invitations/{id}/accept`, `POST /api/invitations/{id}/decline` — view and respond to event invitations.
- `GET /api/notifications`, `POST /api/notifications/{id}/read` — list and mark notifications read.
- `POST /api/events` — publish an event (organizer or admin).
- `GET /api/organizer/events`, `GET /api/organizer/events/{eventId}/registrations` — organizer event and attendee views.
- `POST /api/organizer/registrations/{id}/approve`, `POST /api/organizer/registrations/{id}/reject` — decide pending seat requests.
- `GET /api/organizer/events/{eventId}/invite-candidates`, `POST /api/organizer/events/{eventId}/invitations` — find eligible students and send event invitations.
- `POST /api/organizer/registrations/{registrationId}/attendance` — check in an attendee.
- `GET /api/organizer/events/{eventId}/report.csv` — download a participation report.
- `GET /api/admin/students`, `DELETE /api/admin/students/{studentId}` — list and deactivate student accounts (admin only).
