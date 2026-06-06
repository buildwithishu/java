# College Event Management Portal

A JavaFX desktop application for managing college events, student registrations, certificates, admin analytics, and notifications.

## Tech Stack

| Area | Technology |
|------|------------|
| UI | JavaFX 21 charts and controls, AtlantaFX |
| Backend | Java 17+, JDBC |
| Database | PostgreSQL 16 |
| Build | Maven |
| PDF | Apache PDFBox |
| Local infra | Docker Compose |
| CI | GitHub Actions |

## Quick Start

1. Copy environment defaults:

   ```bash
   cp .env.example .env
   ```

2. Start PostgreSQL and pgAdmin:

   ```bash
   make dev-up
   ```

3. Run the desktop app:

   ```bash
   make app-run
   ```

## Prerequisites

- JDK 17 or newer. CI uses JDK 21.
- Maven 3.9 or newer, or the included `./mvnw` bootstrap script.
- Docker Desktop or Docker Engine for local PostgreSQL.

## Local Services

- PostgreSQL: `localhost:5432`
- Database: `event_portal_db`
- pgAdmin: `http://localhost:5050`
- Default pgAdmin login: `admin@admin.com` / `admin`

The default seeded portal admin account is:

- Username: `admin`
- Password: `admin123`

## Useful Commands

```bash
make help
make dev-up
make dev-down
make db-reset
make app-run
make test
make verify
make app-build
```

`make verify` packages the app, runs the enforced 80% JaCoCo line-coverage gate,
and writes the report to `target/site/jacoco/index.html`. On headless Linux,
install `xvfb`; the Make target uses `xvfb-run` automatically when `DISPLAY` is
not set.

## Project Structure

```text
src/main/java/com/eventportal/
├── App.java
├── Launcher.java
├── controller/
├── dao/
├── model/
├── util/
└── view/

sql/
├── schema.sql
└── seed.sql
```

## Implementation Tracking

Progress against the original implementation plan is tracked in `IMPLEMENTATION_STATUS.md`.

## CI

`.github/workflows/ci.yml` runs compile, tests, package, coverage reporting, and artifact upload on GitHub with a PostgreSQL service container.
