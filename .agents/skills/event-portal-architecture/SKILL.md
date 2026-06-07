---
name: event-portal-architecture
description: Project-specific architecture guidance for the College Event Management Portal.
---

# Event Portal Architecture

Use this skill when implementing or reviewing the College Event Management Portal.

## Source Of Truth

`implementation_plan.md` defines the intended product, stack, layers, database, Docker setup, and CI/CD pipelines. If code and plan conflict before the app is fully scaffolded, call out the conflict and make the smallest change that restores alignment.

## Layer Boundaries

- `model`: plain domain objects and enums. Keep database and JavaFX concerns out.
- `dao`: JDBC persistence with prepared statements, transactions when multiple writes must stay consistent, and try-with-resources.
- `controller`: business rules, validation orchestration, session-aware operations, notifications, and dashboard aggregation.
- `view`: JavaFX scenes and controls. Keep SQL and password logic out.
- `util`: reusable helpers for session, navigation, validation, time formatting, and PDF generation.

## Build Order

1. Maven skeleton, resources, config loading, and test dependencies.
2. PostgreSQL schema, seed data, and Docker Compose.
3. Models and validation utilities.
4. Database connection and DAOs with tests.
5. Auth/session flow with BCrypt.
6. Event and registration business rules with tests.
7. JavaFX views and navigation.
8. PDFBox certificate generation.
9. Dashboards, notifications, CI/CD, and release packaging.

## Domain Rules

- Only admins create, edit, cancel, and report on events.
- Students can register, cancel permitted registrations, view certificates, and read notifications.
- Prevent duplicate active registrations for the same user and event.
- Capacity checks and `registered_count` updates must be consistent in one transaction.
- Completed events should not be edited except through explicit administrative maintenance.
- Certificates should be generated only for confirmed attendance/registration states defined by the application.

## Verification Focus

- Unit test validation and controller rules.
- Integration test DAO operations against disposable PostgreSQL.
- Exercise authentication, registration, cancellation, certificate generation, and notification flows before release.
