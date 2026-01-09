# Janitor Journal

## 2024-05-24: Internationalized User-Facing Strings

- **Issue:** Several user-facing error messages in `ContentActivity` and `MainActivity` were hardcoded strings, preventing localization.
- **Root Cause:** Developers used literal strings directly in `Toast.makeText` and `AlertDialog.Builder` methods.
- **Solution:** Extracted hardcoded strings to `app/src/main/res/values/strings.xml` and replaced usages with `getString(R.string.resource_name)` or `getString(R.string.resource_name, args)` for formatted strings.
- **Pattern:** Always use resource IDs for user-facing text. Use `%1$s`, `%2$d` etc. in string resources for dynamic content.
