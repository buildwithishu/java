---
name: java-design-standards
description: Project-local HLD, LLD, design pattern, and coding-standard workflow for consistent Java implementation.
---

# Java Design Standards

Use this skill before implementing new modules, large classes, or cross-layer changes.

## Required Flow

1. Run `scripts/design-check.sh --init` if `docs/design/HLD.md` or `docs/design/LLD.md` does not exist.
2. Fill HLD before broad architecture changes.
3. Fill LLD before writing classes and methods.
4. Run `scripts/design-check.sh`.
5. Implement with tests using the selected HLD/LLD boundaries.

## High Level Design Standard

HLD must answer:

- What user role or system workflow is being solved?
- Which layers are involved: view, controller, DAO, model, util, database?
- What data moves between layers?
- What security boundaries exist?
- What will be tested?

## Low Level Design Standard

LLD must answer:

- Which package and classes are touched?
- What are the method contracts?
- Which design pattern is used and why?
- What database transaction boundaries apply?
- Which edge cases and tests prove behavior?

## Preferred Patterns For This Project

- DAO pattern for database access.
- Controller/service orchestration for business rules.
- Validation utility for reusable input checks.
- Session manager for current-user lifecycle.
- Navigation manager for JavaFX scene transitions.
- Factory/helper methods for repetitive JavaFX control creation when duplication becomes meaningful.
- Try-with-resources for JDBC, streams, and PDFBox resources.

## Avoid

- SQL in JavaFX views.
- Password or auth logic in views.
- Business rules hidden inside DAO row-mapping code.
- Large classes that mix UI, validation, persistence, and PDF generation.
- Singleton usage except for clear lifecycle owners such as session/navigation/database connection configuration.
