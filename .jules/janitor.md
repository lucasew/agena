
## 2024-07-25 - Refactoring with data loss regression
**Issue:** A refactoring that moved database path logic to a helper class introduced a data-loss bug.
**Root Cause:** The original code used two different filenames for the database: `"history"` for private storage (release builds) and `"history.db"` for external storage (debug builds). During refactoring, the subtle difference was missed, and the new `StorageHelper` class used `"history.db"` for both cases. This would cause the app to look for the wrong filename in private storage, effectively creating a new empty database and making the old one inaccessible.
**Solution:** The fix was to re-introduce a constant for the private database filename (`"history"`) in the `DatabaseController` and use it specifically in the fallback logic for private storage. This preserved the original behavior and prevented data loss.
**Pattern:** When refactoring, pay close attention to seemingly minor details like filenames or string literals, as they can represent important implicit behavior. Seemingly identical constants might have subtle differences that are critical for backward compatibility. Always double-check the original implementation's behavior in all code paths (e.g., debug vs. release, success vs. fallback).

## 2024-07-26 - Simplify Redundant URI Conversion
**Issue:** The `Invoker.java` utility class contained a private method, `getUri(Uri uri)`, that converted a `Uri` object to its string representation and then immediately parsed it back into a `Uri`. This was an unnecessary and inefficient conversion.
**Root Cause:** It was likely a remnant of a previous refactoring or a simple oversight where the code was written without realizing the input was already the correct type.
**Solution:** I removed the `getUri` method entirely. The `getBaseIntent` method was updated to use the input `Uri` object directly, eliminating the redundant `Uri -> String -> Uri` conversion.
**Pattern:** Avoid redundant type conversions. If a variable is already in the required type, use it directly instead of converting it to a primitive and back again. This simplifies the code, improves readability, and can offer a minor performance benefit.
