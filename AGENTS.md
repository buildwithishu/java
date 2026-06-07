# College Event Management Portal Agent Guide

This project is planned as a JavaFX desktop app backed by PostgreSQL and Maven. Use `implementation_plan.md` as the product and architecture source of truth until the application code exists.

## Agent-First Workflow

Start non-trivial work by choosing the narrowest agent role and the relevant project skill:

- `planner`: break implementation plan sections into small phases and dependencies.
- `tdd-guide`: write or update tests before production code.
- `java-builder`: Maven, Java 17+/21, dependency, packaging, and JavaFX build work.
- `database-reviewer`: PostgreSQL schema, migrations, DAO queries, indexes, constraints, and seed data.
- `security-reviewer`: authentication, password hashing, input validation, secrets, SQL injection, file paths, and certificate generation.
- `docker-ci`: Docker Compose, Dockerfile, GitHub Actions, release, and GHCR workflows.
- `design-reviewer`: HLD/LLD, design patterns, modularity, and coding-standard consistency.
- `code-reviewer`: review code after changes and before commit.

Prefer parallel agent review only when tasks are independent, such as database schema review and CI workflow review. Keep implementation edits in the main session unless explicitly delegating through Codex multi-agent features.

## Installed Project Skills

Marketplace skills installed with `npx skills`:

- `java-junit`: primary JUnit 5 testing guidance.
- `java-maven`: Maven POM, lifecycle, plugin, and dependency guidance.
- `java-testing`: secondary Java testing guidance. Treat as advisory because the Skills CLI flagged generated content as higher risk.
- `124-java-secure-coding`: Java secure coding guidance.
- `postgres`: PostgreSQL guidance for schemas and queries.
- `docker-ci-cd`: Docker and CI/CD guidance. Treat as advisory and verify against official Docker/GitHub Actions behavior.
- `java-pro`: Java design and implementation guidance.
- `system-design`: high-level system design guidance. Treat as advisory because marketplace install metadata was limited.
- `balanced-coupling`: modularity and coupling guidance.

Project-specific skills:

- `event-portal-architecture`: domain, layers, data model, and implementation order.
- `javafx-pdfbox-desktop`: JavaFX desktop UI and PDFBox certificate guidance.
- `java-design-standards`: HLD/LLD workflow and Java design pattern standards.

## Design Scripts

- `scripts/design-check.sh --init`: create `docs/design/HLD.md` and `docs/design/LLD.md` from templates.
- `scripts/design-check.sh`: validate that HLD/LLD docs contain the required sections and no placeholder title values.

Use these before implementing a new module or cross-layer feature.

## Development Rules

- Use Java 17+ language features compatible with the plan; CI should build with JDK 21.
- Use Maven standard layout: `src/main/java`, `src/main/resources`, `src/test/java`.
- Keep UI, controller/service, DAO, model, and utility layers separate.
- Do not hardcode secrets. Runtime credentials must come from environment variables or `config.properties` defaults intended only for local development.
- Use parameterized SQL or prepared statements for every database input.
- Hash passwords with BCrypt; never store or log plaintext passwords.
- Keep PDF certificate output paths constrained to the configured certificate directory.
- Add tests with each feature. Favor unit tests for validation/service logic and integration tests for DAO behavior against PostgreSQL.
- For Docker-first local development, PostgreSQL belongs in Docker Compose; the JavaFX GUI runs on the developer machine with `mvn javafx:run`.

## Verification

Run the closest available checks after changes:

- `mvn test` for unit tests.
- `mvn test -P headless` for CI/headless test behavior once the profile exists.
- `mvn package -DskipTests` for packaging.
- `docker compose config` after Compose edits.
- `docker build .` after Dockerfile edits.

If the project is still being scaffolded and a command cannot run yet, state that clearly in the final response.
