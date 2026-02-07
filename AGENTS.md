# AGENTS.md

This file documents the conventions and workflows for AI agents and developers working on this repository.

## Mise First Policy

- **Mise is Mandatory:** All development tasks must be executed via `mise`.
- **Installation:** If `mise` is missing, install it: `curl https://mise.jdx.dev/install.sh | sh`.
- **Configuration:** The `mise.toml` file is the source of truth for tools and tasks.
- **Execution:** Use `mise run [task]` (e.g., `mise run lint`, `mise run test`). Do not run `gradlew` or other tools directly unless debugging `mise` itself.

## CI/CD Workflow

The project uses a single GitHub Actions workflow: `.github/workflows/autorelease.yml`.

### Workflow Steps

1. **Install:** `mise run install`
2. **Codegen:** `mise run codegen`
3. **PR Check:** If codegen results in changes, a PR is automatically created.
4. **CI:** `mise run ci` (Lint + Test + Build)
5. **Release:** If dispatch/tag, `gh release create` is executed.
6. **Artifacts:** Uploads APKs and test reports.

## Linting Standards

- **Shell Scripts:**
  - Linter: `shellcheck`
  - Formatter: `shfmt`
  - Configuration: Default via `mise`.
- **Markdown:**
  - Linter: `markdownlint-cli2`
  - Configuration: `.markdownlint.yaml` (MD013, MD033, MD041 disabled).
- **TOML:**
  - Linter: `taplo`
  - Formatter: `taplo fmt`
  - Configuration: Default via `mise`.
- **Android (Kotlin/Java):**
  - Linter: Android Lint (`lint:android`)
  - Formatter: Standard Android formatting.

## programmatic checks

- Use `mise run ci` to verify all checks pass locally.
