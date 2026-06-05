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
| Docker local database | Not started | PostgreSQL 16 and pgAdmin via Compose |
| Database schema | In progress | Core tables, constraints, indexes, admin seed, and sample seed data added |
| Models | Not started | Domain entities and enums |
| DAO layer | Not started | JDBC persistence and tests |
| Controller/service layer | Not started | Auth, events, registrations, certificates, notifications, dashboards |
| JavaFX views | Not started | Login, dashboards, event screens, certificates, notifications |
| Utilities | Not started | Session, navigation, validation, date/time, PDF generation |
| CI/CD | Not started | GitHub Actions for CI, release, Docker build |
| Documentation | Not started | README, contributing, PR template |

## Implementation Log

- 2026-06-05: Started product implementation tracking.
- 2026-06-05: Added Maven/JavaFX baseline with launcher, config resource, and smoke test.
- 2026-06-05: Verification note: Java 21 is installed; `mvn test` could not run because `mvn` is not installed.
- 2026-06-05: Added PostgreSQL schema and seed data for users, events, registrations, certificates, and notifications.
