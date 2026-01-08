
## 2024-07-25 - Refactoring with data loss regression
**Issue:** A refactoring that moved database path logic to a helper class introduced a data-loss bug.
**Root Cause:** The original code used two different filenames for the database: `"history"` for private storage (release builds) and `"history.db"` for external storage (debug builds). During refactoring, the subtle difference was missed, and the new `StorageHelper` class used `"history.db"` for both cases. This would cause the app to look for the wrong filename in private storage, effectively creating a new empty database and making the old one inaccessible.
**Solution:** The fix was to re-introduce a constant for the private database filename (`"history"`) in the `DatabaseController` and use it specifically in the fallback logic for private storage. This preserved the original behavior and prevented data loss.
**Pattern:** When refactoring, pay close attention to seemingly minor details like filenames or string literals, as they can represent important implicit behavior. Seemingly identical constants might have subtle differences that are critical for backward compatibility. Always double-check the original implementation's behavior in all code paths (e.g., debug vs. release, success vs. fallback).

## 2024-07-26 - Prevent Redundant Security Provider Registration
**Issue:** The `SecurityProvider.addConscryptIfAvailable()` method unconditionally inserted the Conscrypt security provider every time it was called. While it was called within a singleton, this approach is not robust and could lead to unnecessary re-registration attempts.
**Root Cause:** The initial implementation did not check whether the security provider was already registered before attempting to add it.
**Solution:** The method was updated to first check if the "Conscrypt" provider is already available using `Security.getProvider("Conscrypt") == null`. The provider is now only inserted if it is not already registered, making the initialization logic more efficient and resilient.
**Pattern:** Before registering global resources, such as security providers or listeners, always check for their existence to prevent redundant operations and potential side effects of multiple registrations.
