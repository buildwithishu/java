# Contributing

## Workflow

1. Create a feature branch from `main`.
2. Keep changes small and focused.
3. Update `IMPLEMENTATION_STATUS.md` when implementation scope changes.
4. Run the closest available verification command.
5. Open a pull request using the template.

## Commit Style

Use conventional commits:

```text
feat: add event registration flow
fix: handle duplicate registration
chore: update docker compose setup
test: add validation tests
docs: document local setup
```

## Quality Expectations

- Use prepared statements for SQL.
- Validate inputs at UI/controller boundaries.
- Do not hardcode secrets.
- Keep JavaFX views separate from DAO/database logic.
- Add or update tests for business rules and utilities.
- Run `make verify` before opening a pull request; the build enforces 80% line coverage.
- Install `xvfb` on headless Linux so JavaFX screen and interaction tests can run.
