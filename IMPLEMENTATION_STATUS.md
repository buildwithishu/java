# Implementation Status

This file tracks implementation progress against `implementation_plan.md`.

## Commit Policy

- Make small atomic commits.
- Commit only product/application files.
- Do not commit agent-development workflow files such as `.agents/`, `.codex/`, `AGENTS.md`, or `skills-lock.json`.

## Progress

| Area | Status | Notes |
|------|--------|-------|
| Maven project skeleton | In progress | Java 17+, JavaFX 21, Maven packaging scaffolded; Unix Maven bootstrapper added |
| Docker local database | In progress | Compose, test Compose, Dockerfile, env template, and Makefile added |
| Database schema | In progress | Core tables, constraints, indexes, admin seed, and sample seed data added |
| Models | In progress | Immutable records and enums added |
| DAO layer | In progress | User, event, registration, certificate, and notification DAOs added |
| Controller/service layer | In progress | Auth, event, registration, notification, certificate, and dashboard controller foundations added |
| JavaFX views | In progress | Login/register, student dashboard, admin dashboard, event list, registrations, certificates, notifications, profile, admin event management, reports, and dashboard placeholder added |
| Utilities | In progress | Validation, date/time, session, navigation, password, and PDF certificate helpers added |
| CI/CD | In progress | CI, release, and Docker build workflows added |
| Documentation | In progress | README, contributing guide, and PR template added |

## Implementation Log

- 2026-06-05: Started product implementation tracking.
- 2026-06-05: Added Maven/JavaFX baseline with launcher, config resource, and smoke test.
- 2026-06-05: Verification note: Java 21 is installed; `mvn test` could not run because `mvn` is not installed.
- 2026-06-05: Added PostgreSQL schema and seed data for users, events, registrations, certificates, and notifications.
- 2026-06-05: Added Docker-first local database setup, test database Compose file, Dockerfile, env example, and Makefile.
- 2026-06-05: Added initial immutable domain models, enums, validation utilities, date/time helpers, and validation tests.
- 2026-06-05: Verified model/util source compilation with `javac`; full Maven tests still blocked by missing `mvn`.
- 2026-06-05: Added environment-aware app configuration loader and JDBC connection manager with config tests.
- 2026-06-05: Verified model/util/DAO foundation source compilation with `javac`; full Maven tests still blocked by missing `mvn`.
- 2026-06-05: Added UserDAO and EventDAO with prepared statements, CRUD/search methods, and row mappers.
- 2026-06-05: Verified DAO source compilation with `javac`; full Maven tests still blocked by missing `mvn`.
- 2026-06-05: Added BCrypt password helper, session manager, and AuthController for login/student registration.
- 2026-06-05: Verified dependency-free backend subset with `javac`; BCrypt-backed auth requires Maven for full compile/test.
- 2026-06-05: Added EventController with admin-only event creation, updates, cancellation, search, and validation.
- 2026-06-05: Verified EventController source compilation with dependency-free backend subset using `javac`.
- 2026-06-05: Added RegistrationDAO, CertificateDAO, and NotificationDAO with prepared statements and transactional registration count updates.
- 2026-06-05: Verified expanded DAO/backend subset compilation with `javac`; full Maven tests still blocked by missing `mvn`.
- 2026-06-05: Added RegistrationController and NotificationController for student registration, cancellation, admin registration lookup, and notifications.
- 2026-06-05: Verified registration/notification controller source compilation with dependency-free backend subset using `javac`.
- 2026-06-05: Added PDF certificate generation utility and CertificateController for generating and saving certificate records.
- 2026-06-05: Verified non-PDFBox backend subset with `javac`; full Maven/PDFBox verification is blocked because local `mvn` is missing and Docker daemon is unavailable.
- 2026-06-05: Added dashboard summary models and DashboardController for admin event, registration, student, category, and popular-event stats.
- 2026-06-05: Verified dashboard/backend subset compilation with `javac`; full Maven tests still blocked by missing `mvn`.
- 2026-06-05: Added GitHub Actions CI workflow with JDK 21, PostgreSQL service, Maven test/package, and artifact uploads.
- 2026-06-05: Added JavaFX navigation manager, login/register views, dashboard placeholder, app wiring, and CSS.
- 2026-06-05: Verification note: JavaFX UI compile/test remains blocked locally because `mvn` is missing and Docker daemon is unavailable.
- 2026-06-05: Added student dashboard and event list view with registration action wiring.
- 2026-06-05: Verification note: student dashboard UI depends on JavaFX/Maven; local `mvn` and Docker daemon remain unavailable.
- 2026-06-05: Added admin dashboard view with summary stats, category distribution, and popular-event list.
- 2026-06-05: Verification note: admin dashboard UI depends on JavaFX/Maven; local `mvn` and Docker daemon remain unavailable.
- 2026-06-05: Added admin event management view and event creation form with create/cancel event actions.
- 2026-06-05: Verification note: admin event management UI depends on JavaFX/Maven; local `mvn` remains unavailable.
- 2026-06-05: Added README, contributing guide, and pull request template.
- 2026-06-05: Added GitHub Actions release and Docker image build workflows.
- 2026-06-05: Added student registrations, certificates, and notifications views and wired sidebar navigation.
- 2026-06-05: Verification note: student registrations/certificates/notifications UI depends on JavaFX/Maven; local `mvn` remains unavailable.
- 2026-06-05: Added registration detail model/query so student registration cards show event title, date, and venue.
- 2026-06-05: Verified registration detail backend subset with `javac`; full Maven/UI verification remains blocked by missing `mvn`.
- 2026-06-05: Updated registration schema to enforce unique confirmed registrations while allowing re-registration after cancellation.
- 2026-06-05: Fixed certificate generation to render the registration owner's name instead of whichever user triggered generation.
- 2026-06-05: Verified backend subset with `javac`; full certificate compile remains Maven/PDFBox-bound.
- 2026-06-05: Added student profile view and sidebar route.
- 2026-06-05: Verification note: profile UI depends on JavaFX/Maven; local `mvn` remains unavailable.
- 2026-06-05: Added admin report view with dashboard summary text output.
- 2026-06-05: Verification note: admin report UI depends on JavaFX/Maven; local `mvn` remains unavailable.
- 2026-06-05: Added event status transitions and cancellation notifications for active registrants.
- 2026-06-05: Verified event lifecycle backend subset with `javac`; full JavaFX/Maven verification remains blocked by missing `mvn`.
- 2026-06-05: Hardened certificate-ready notifications to avoid duplicates and added validated old-notification cleanup.
- 2026-06-05: Verified notification hardening backend subset with `javac`; full Maven/PDFBox verification remains blocked by missing `mvn`.
- 2026-06-05: Added Unix Maven bootstrapper script so the project can run Maven without a preinstalled `mvn`.
- 2026-06-05: Verification note: `./mvnw -v` could not complete because Maven binary download from Apache archive stalled in this environment.
