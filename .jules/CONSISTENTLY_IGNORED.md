# Consistently Ignored Changes

This file lists patterns of changes that have been consistently rejected by human reviewers. All agents MUST consult this file before proposing a new change. If a planned change matches any pattern described below, it MUST be abandoned.

---

## IGNORE: Caching the results of permission checks

**- Pattern:** Do not introduce any form of caching for Android permission check results (e.g., `checkSelfPermission`).
**- Justification:** This pattern has been rejected multiple times. Android permissions can be granted or revoked by the user at any time through the system settings. Caching the result of a permission check can lead to the application having a stale and incorrect understanding of its permissions, causing it to either fail unexpectedly or violate user privacy by accessing data after permission has been revoked. The only source of truth is a real-time check with the Android OS.
**- Files Affected:** Potentially any file dealing with Android runtime permissions.

---

## IGNORE: Forcing Node 24 in GitHub Actions

**- Pattern:** Adding `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` to `.github/workflows/autorelease.yml`.
**- Justification:** This environment variable override is frequently rejected because the project relies on specific versions of GitHub actions (like `actions/checkout` v3.1.0) that are expected to run in their native environments. Forcing Node 24 causes unintended side effects and breaks the CI pipeline.
**- Files Affected:** `.github/workflows/autorelease.yml`

---

## IGNORE: Removing unused `app_description` string resource

**- Pattern:** Removing `<string name="app_description">` from `strings.xml` or related localization files.
**- Justification:** Although it appears unused to static analysis tools, removing `app_description` is consistently rejected. It may be used dynamically, by external tools, or is planned for future use. Removing it causes unnecessary churn and localization issues.
**- Files Affected:** `app/src/main/res/values*/strings.xml`

---

## IGNORE: Agent Scope Creep

**- Pattern:** PRs where an agent (e.g., Janitor, Docs, Denoiser) modifies files outside its strictly defined persona scope. For example, a Docs or Denoiser agent modifying runtime logic or configuration files like `mise.toml`.
**- Justification:** Each agent persona has a specific purpose. Scope creep violates the operational contract and mixes concerns, making PRs harder to review and riskier to merge. Changes must align strictly with the agent's stated mission.
**- Files Affected:** Any file outside the agent's explicit allowed paths or logical scope.

---

## IGNORE: Unpinned Mise Tools

**- Pattern:** Using "latest", "lts", or loose version constraints (e.g., `node = "lts"`, `markdownlint-cli2 = "latest"`) in `mise.toml`.
**- Justification:** CI/CD requires deterministic environments. Tools must be pinned to exact versions (e.g., `node = "22.12.0"`) to ensure stability and reproducibility.
**- Files Affected:** `mise.toml`

---

## IGNORE: Manual URI Parsing and Resolution

**- Pattern:** Manually parsing or resolving URIs using `java.net.URI.create()` or `resolve()` (often with try-catch blocks) in Activities or Fragments.
**- Justification:** Use `com.biglucas.agena.protocol.gemini.GeminiUriHelper`, which encapsulates URI logic, sanitization, and exception handling. Ad-hoc implementations are error-prone and redundant.
**- Files Affected:** `GeminiPageContentFragment.java`, `PageActivity.java`, `Gemini.java`

---

## IGNORE: Verbose Janitor Journal Entries

**- Pattern:** Adding multi-line, verbose entries (with `##` headers) to `.jules/janitor.md`.
**- Justification:** The project standards strictly require a single-line format: `- YYYY-MM-DD: [pattern]`. Verbose entries clutter the file and violate the convention.
**- Files Affected:** `.jules/janitor.md`

---

## IGNORE: Over-Centralizing Error Reporting

**- Pattern:** Attempting to create an `ErrorReporter.java` or similar centralized error-handling utility and retroactively replacing functional `Log.e` or exception-handling blocks across the codebase.
**- Justification:** Despite some generic AI agent instructions suggesting centralized error reporting, these sweeping refactoring PRs are consistently rejected in this repository as they introduce unnecessary churn, complicate simple logging, and often break existing error flows.
**- Files Affected:** `app/src/main/java/com/biglucas/agena/utils/ErrorReporter.java` (and files modified to use it).

---

## IGNORE: Retrofitting Extensive Javadoc to Internal Components

**- Pattern:** Adding extensive Javadoc comments to existing internal activities (e.g., `ContentActivity.java`, `PageActivity.java`) or internal protocol implementations.
**- Justification:** "Docs" agents frequently propose adding verbose Javadoc to internal components that don't represent public APIs. These PRs are rejected because they create code noise without adding equivalent value for a simple internal codebase.
**- Files Affected:** `app/src/main/java/**/*.java`

---

## IGNORE: Moving DatabaseController without architectural mandate

**- Pattern:** Moving `DatabaseController.java` from `com.biglucas.agena.utils` to `com.biglucas.agena.db`.
**- Justification:** Refactor PRs attempting to implement "domain-driven structure" by moving this file are consistently rejected. The current location is considered stable, and moving it creates unnecessary package churn and requires updating all imports across the app for little practical benefit.
**- Files Affected:** `DatabaseController.java` and all files importing it.

---
