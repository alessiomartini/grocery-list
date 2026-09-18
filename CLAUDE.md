# Claude Code instructions

## Project

Android grocery-list application with a backend component. Keep app, backend,
and shared data responsibilities separate.

## Verification

- Read `README.md` and the affected module before editing.
- Use the checked-in Gradle wrapper: `.\gradlew.bat <task>`.
- Run the narrowest relevant tests/checks, then build the affected module.
- Keep credentials, local configuration, keystores, and generated builds out of git.

## Workflow

- Preserve persistence and migration behavior.
- Use fake/seed data for local verification; never use real personal data.
- Inspect `git diff` and report the exact Gradle tasks run before committing.
