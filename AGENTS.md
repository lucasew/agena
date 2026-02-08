# AGENTS.md

This file contains instructions for AI agents working on this repository.

## Tooling and Environment

- **Mise First:** You MUST use `mise` for all task execution. Do not rely on global tools.
- **Tasks:** Use `mise run <task>` to execute tasks.
  - `mise run lint`: Runs all linters (ShellCheck, Android Lint, MarkdownLint, Taplo).
  - `mise run fmt`: Runs formatters (shfmt, Taplo).
  - `mise run test`: Runs tests.
  - `mise run codegen`: Runs code generation (SQLC).
  - `mise run ci`: Runs the full CI pipeline (Lint + Test + Build).

## CI/CD Workflow

The project uses a single GitHub Actions workflow (`.github/workflows/autorelease.yml`) that follows this flow:

1. `mise run install`
2. `mise run codegen` -> If changes, PR is created.
3. `mise run ci`
4. Release and Artifact upload (if applicable).

## Linting Standards

- **Shell:** `shellcheck` and `shfmt`.
- **Android:** Standard Android Lint.
- **Markdown:** `markdownlint-cli2`.
- **TOML:** `taplo`.

## Code Conventions

- **Consistently Ignored Patterns:** Consult `.jules/CONSISTENTLY_IGNORED.md` before proposing changes.
- **Error Handling:** Use centralized error reporting. Never swallow exceptions silently.
- **Testing:** Tests must cover actual changes. Avoid redundant tests.
