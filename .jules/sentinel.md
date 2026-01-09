## PERMANENT DIRECTIVE - DO NOT ALTER
**Vulnerability:** The application is configured with `android:allowBackup="true"`.
**Learning:** This configuration is intentional and has been explicitly approved. Per user directive, the `android:allowBackup` flag MUST NOT be disabled.
**Prevention:** Sentinel agents are permanently instructed to ignore the `android:allowBackup` setting in this repository. Do not flag it as a vulnerability or create a pull request to change it. This is a standing project-level decision.

## 2024-07-22 - Prevent stack trace leakage in PageActivity
**Vulnerability:** Unhandled exceptions in `PageActivity` were printed directly to the console using `e.printStackTrace()`, leaking potentially sensitive application internals.
**Learning:** The codebase has a recurring issue with uncaught exceptions leaking stack traces. A utility class, `StacktraceDialogHandler`, was previously created to standardize error handling and present a user-friendly dialog.
**Prevention:** All new exception handling should use the `StacktraceDialogHandler.show(context, e)` method instead of `e.printStackTrace()` to ensure consistent and secure error reporting. Future code reviews should flag any direct usage of `e.printStackTrace()`.

## 2024-07-23 - Prevent Denial of Service in ContentActivity
**Vulnerability:** The `ContentActivity` was vulnerable to a Denial of Service (DoS) attack. It would read the entire contents of a file passed via an Intent into memory without first checking the file's size. A malicious application could provide a very large file, causing the application to crash with an `OutOfMemoryError`.
**Learning:** Exported Android Activities that handle file or data streams from external sources are a common attack surface. The lack of input validation, specifically checking the size of incoming data before processing, led to this vulnerability. The existing `MAX_LINES` check was insufficient as a large file might not contain many newline characters.
**Prevention:** Before reading any data stream from an external Intent, always use the `ContentResolver` to query the `OpenableColumns.SIZE` of the content URI. Enforce a reasonable maximum size limit and reject any content that exceeds it. This prevents the application from attempting to load excessively large files into memory.

## 2026-01-09 - Mitigate Information Disclosure in Global Exception Handler
**Vulnerability:** The global exception handler, `StacktraceDialogHandler`, was displaying full Java stack traces directly to the user in an alert dialog. This is an information disclosure vulnerability (CWE-209) that reveals internal application structure, library versions, and file paths, which could aid an attacker in formulating more targeted attacks.
**Learning:** The application's README file explicitly mentioned showing the full stack trace as a feature, indicating this was an intentional but insecure design choice. This highlights the need to prioritize security over transparent but risky debugging features in production code. The vulnerability was not in a single activity but in the centralized handler used by all activities.
**Prevention:** Error handling mechanisms must differentiate between user-facing messages and developer-facing logs. Sensitive details, including stack traces, should be logged to a secure, developer-accessible location (like Logcat), while users should only see a generic, non-technical error message. This prevents the leakage of internal application details. A sanitized, minimal error report can be provided for users to copy if needed.
