# Consistently Ignored Changes

This file lists patterns of changes that have been consistently rejected by human reviewers. All agents MUST consult this file before proposing a new change. If a planned change matches any pattern described below, it MUST be abandoned.

---

## IGNORE: Caching the results of permission checks

**- Pattern:** Do not introduce any form of caching for Android permission check results (e.g., `checkSelfPermission`).
**- Justification:** This pattern has been rejected multiple times. Android permissions can be granted or revoked by the user at any time through the system settings. Caching the result of a permission check can lead to the application having a stale and incorrect understanding of its permissions, causing it to either fail unexpectedly or violate user privacy by accessing data after permission has been revoked. The only source of truth is a real-time check with the Android OS.
**- Files Affected:** Potentially any file dealing with Android runtime permissions.

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

## IGNORE: Janitor Scope Creep

**- Pattern:** Janitor PRs that introduce logic changes (e.g., `@RequiresApi`, `try-with-resources`), refactor code structure (e.g., changing method signatures), or add features/security fixes.
**- Justification:** Janitor tasks are strictly for non-functional cleanups (e.g., removing unused imports, fixing typos, formatting). Any change to runtime behavior or API structure belongs in a Refactor or Sentinel task.
**- Files Affected:** Any file touched by a Janitor PR.

---

## IGNORE: Verbose Janitor Journal Entries

**- Pattern:** Adding multi-line, verbose entries (with `##` headers) to `.jules/janitor.md`.
**- Justification:** The project standards strictly require a single-line format: `- YYYY-MM-DD: [pattern]`. Verbose entries clutter the file and violate the convention.
**- Files Affected:** `.jules/janitor.md`

---
