
## 2024-07-25 - Refactoring with data loss regression
**Issue:** A refactoring that moved database path logic to a helper class introduced a data-loss bug.
**Root Cause:** The original code used two different filenames for the database: `"history"` for private storage (release builds) and `"history.db"` for external storage (debug builds). During refactoring, the subtle difference was missed, and the new `StorageHelper` class used `"history.db"` for both cases. This would cause the app to look for the wrong filename in private storage, effectively creating a new empty database and making the old one inaccessible.
**Solution:** The fix was to re-introduce a constant for the private database filename (`"history"`) in the `DatabaseController` and use it specifically in the fallback logic for private storage. This preserved the original behavior and prevented data loss.
**Pattern:** When refactoring, pay close attention to seemingly minor details like filenames or string literals, as they can represent important implicit behavior. Seemingly identical constants might have subtle differences that are critical for backward compatibility. Always double-check the original implementation's behavior in all code paths (e.g., debug vs. release, success vs. fallback).

## 2024-07-26 - Optimize getAcceptedIssuers to Avoid Unnecessary Allocations
**Issue:** The `getAcceptedIssuers()` method in `GeminiTrustManager` created a new `X509Certificate[0]` array on every invocation. This is a small but unnecessary memory allocation that can be easily avoided.
**Root Cause:** The implementation was likely written without considering the performance impact of repeated allocations of an empty, immutable array. It's a common oversight in code that is not performance-critical.
**Solution:** I replaced the `return new X509Certificate[0];` with `return ACCEPTED_ISSUERS;` where `ACCEPTED_ISSUERS` is a `private static final X509Certificate[]` initialized once.
**Pattern:** For methods that return empty, immutable collections or arrays, cache and reuse a single `static final` instance to prevent unnecessary memory allocations and reduce pressure on the garbage collector. This is a common and effective micro-optimization in Java.
