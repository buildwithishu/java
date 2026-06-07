# Low Level Design: Portal Baseline Layering

## Scope

- Package: `com.eventportal`.
- Classes touched: models, DAOs, controllers, views, and utilities defined in `implementation_plan.md`.
- Public behavior: role-based event management, registration, certificates, notifications, and dashboards.

## Class Design

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `DatabaseConnection` | Load database config and provide JDBC connections | `getConnection`, `close` |
| `AuthController` | Login, registration, session flow | `login`, `register`, `logout` |
| `EventController` | Event business rules | `createEvent`, `updateEvent`, `findUpcoming` |
| `RegistrationController` | Registration and cancellation rules | `registerForEvent`, `cancelRegistration` |

## Method Contracts

### `controllerMethod`

- Input: validated DTO-like method parameters or model instances.
- Output: model, boolean/result object, or user-facing status.
- Validation: required fields, role, capacity, date/time, duplicate registration.
- Errors: no secret leakage; user-friendly messages at view boundary.
- Side effects: database writes, notifications, certificate file creation where relevant.
- Tests: unit test rule branches and integration test persistence behavior.

## Patterns Used

- DAO/Repository: one DAO per persistent aggregate.
- Service/Controller orchestration: controllers coordinate validation and DAO calls.
- Singleton or lifecycle owner: session, navigation, and database configuration only.
- Factory/builder/helper: reusable JavaFX control creation and PDF layout helpers when duplication appears.

## Data Structures

- Java domain objects for users, events, registrations, certificates, and notifications.
- Enums for role, event category, event status, and registration status.

## Database Details

- Tables: `users`, `events`, `registrations`, `certificates`, `notifications`.
- Queries: CRUD, search/filter, dashboard aggregates, registration lookups.
- Transaction boundary: registration/cancellation updates event counts and registration rows atomically.
- Index/constraint expectations: unique usernames/emails, duplicate registration prevention, foreign keys.

## Coding Standard Checklist

- Small methods with one responsibility.
- Clear names for classes, methods, variables, and constants.
- No UI logic in DAO/model classes.
- No SQL in JavaFX views.
- Prepared statements for all user-supplied values.
- No hardcoded secrets.
- BCrypt for password operations.
- Try-with-resources for JDBC, streams, and PDFBox resources.
- Tests added or updated before implementation where practical.

## Edge Cases

- Full event capacity.
- Duplicate registration.
- Invalid role access.
- Missing or invalid database configuration.
- Certificate path conflicts.

## Test Cases

| Test | Type | Expected Result |
|------|------|-----------------|
| Validate email and password | Unit | invalid input rejected |
| Register for available event | Integration | registration row and event count update |
| Register for full event | Unit/Integration | registration rejected |
| Generate certificate PDF | Unit | non-empty PDF is created |
