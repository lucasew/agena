# Consistently Ignored Changes

This file lists patterns of changes that have been consistently rejected by human reviewers. All agents MUST consult this file before proposing a new change. If a planned change matches any pattern described below, it MUST be abandoned.

---

## IGNORE: Caching Permission Check Results

**- Pattern:** Do not introduce caching for the results of permission checks.
**- Justification:** This change has been rejected multiple times. Caching permission statuses is risky because permissions can be revoked by the user at any time via the system settings. A stale cache could lead to incorrect application behavior, security vulnerabilities (if a permission is assumed to be granted when it is not), or a degraded user experience. The Android OS provides an efficient and definitive way to check permissions at runtime, and this should be the single source of truth.
**- Files Affected:** `app/src/main/java/com/biglucas/agena/utils/RuntimePermissions.java`

---
