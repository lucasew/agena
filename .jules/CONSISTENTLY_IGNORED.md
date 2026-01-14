# Consistently Ignored Changes

This file lists patterns of changes that have been consistently rejected by human reviewers. All agents MUST consult this file before proposing a new change. If a planned change matches any pattern described below, it MUST be abandoned.

---

## IGNORE: Caching the results of permission checks

**- Pattern:** Do not introduce any form of caching for Android permission check results (e.g., `checkSelfPermission`).
**- Justification:** This pattern has been rejected multiple times. Android permissions can be granted or revoked by the user at any time through the system settings. Caching the result of a permission check can lead to the application having a stale and incorrect understanding of its permissions, causing it to either fail unexpectedly or violate user privacy by accessing data after permission has been revoked. The only source of truth is a real-time check with the Android OS.
**- Files Affected:** Potentially any file dealing with Android runtime permissions.

---
