# Implementation Status

This file tracks implementation progress against `implementation_plan.md`.

## Commit Policy

- Make small atomic commits.
- Commit only product/application files.
- Do not commit agent-development workflow files such as `.agents/`, `.codex/`, `AGENTS.md`, or `skills-lock.json`.

## Progress

| Area | Status | Notes |
|------|--------|-------|
| Maven project skeleton | In progress | Java 17+, JavaFX 21, Maven packaging scaffolded |
| Docker local database | In progress | Compose, test Compose, Dockerfile, env template, and Makefile added |
| Database schema | In progress | Core tables, constraints, indexes, admin seed, and sample seed data added |
| Models | In progress | Immutable records and enums added |
| DAO layer | In progress | Configuration loader, database connection manager, UserDAO, and EventDAO added |
| Controller/service layer | Not started | Auth, events, registrations, certificates, notifications, dashboards |
| JavaFX views | Not started | Login, dashboards, event screens, certificates, notifications |
| Utilities | In progress | Validation and date/time helpers added |
| CI/CD | Not started | GitHub Actions for CI, release, Docker build |
| Documentation | Not started | README, contributing, PR template |

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
