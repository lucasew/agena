
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

## 2024-07-26 - Prevent Redundant Security Provider Registration
**Issue:** The `SecurityProvider.addConscryptIfAvailable()` method unconditionally inserted the Conscrypt security provider every time it was called. While it was called within a singleton, this approach is not robust and could lead to unnecessary re-registration attempts.
**Root Cause:** The initial implementation did not check whether the security provider was already registered before attempting to add it.
**Solution:** The method was updated to first check if the "Conscrypt" provider is already available using `Security.getProvider("Conscrypt") == null`. The provider is now only inserted if it is not already registered, making the initialization logic more efficient and resilient.
**Pattern:** Before registering global resources, such as security providers or listeners, always check for their existence to prevent redundant operations and potential side effects of multiple registrations.

## 2024-07-29 - Remove Redundant URI Conversion
**Issue:** The `Invoker.java` utility class contained a `getUri(Uri uri)` method that converted a `Uri` object to a `String` and immediately parsed it back into a `Uri`. This operation was redundant and inefficient.
**Root Cause:** This was likely a leftover from a previous refactoring or a simple oversight where the code was written to handle a `String` before being changed to accept a `Uri`, but the conversion logic was never removed.
**Solution:** I removed the `getUri` method entirely. The `getBaseIntent` method, which was the only place it was used, was updated to use the input `Uri` object directly. This simplifies the code and removes an unnecessary object allocation and parsing step.
**Pattern:** Avoid redundant type conversions, such as `Uri -> String -> Uri`. If a method already has an object in the correct type, use it directly instead of converting it back and forth. This improves both performance and code clarity.

## 2026-01-09 - Replace `System.out.printf` with `Log.d` for Android Best Practices
**Issue:** The `PermissionAsker` utility class used `System.out.printf` for logging debug information, which is not standard practice in Android development.
**Root Cause:** This was likely a remnant of quick, C-style debugging or code written by a developer less familiar with Android-specific logging conventions.
**Solution:** I replaced the `System.out.printf` call with a standard `Log.d` call, which integrates the output with Android's `logcat` system. This required adding a `TAG` constant to the class and importing `android.util.Log`.
**Pattern:** Always use the standard Android `Log` class (`Log.d`, `Log.i`, `Log.e`, etc.) for logging instead of `System.out.print`. This ensures log messages are correctly routed to `logcat`, can be filtered by tags, and can be compiled out of release builds.
## 2026-01-10 - Cache Expensive, Static Lookups
**Issue:** The `DebugUIHelper.hasManageExternalStoragePermission` method repeatedly queried the `PackageManager` every time it was called. This is an expensive operation for a value that does not change during the application's runtime.
**Root Cause:** The implementation was not optimized to cache the result of the permission check, leading to unnecessary work on every call.
**Solution:** I introduced a `private static Boolean canShowUI` field to cache the result of the permission check. The `canShowDebugUI` method (renamed for clarity) now checks if the value is already cached and returns it, otherwise it performs the lookup once and stores the result for future calls.
**Pattern:** When a value is expensive to compute or look up and remains static throughout the application's lifecycle (like runtime permissions), cache it in a `static` field to avoid redundant work and improve performance.
