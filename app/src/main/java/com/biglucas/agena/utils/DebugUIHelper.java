package com.biglucas.agena.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * Helper class for handling debug-only UI elements and behavior.
 * <p>
 * <b>Architecture Note:</b> This application uses the {@code MANAGE_EXTERNAL_STORAGE} permission
 * as a proxy for "Developer Mode" or "Debug Mode". This is an intentional deviation from
 * checking {@code BuildConfig.DEBUG} to allow enabling advanced features (like full logging
 * or external database access) in release builds for trusted users/developers.
 */
public class DebugUIHelper {
    private static final String TAG = "DebugUIHelper";

    private DebugUIHelper() {
        // This is a utility class and should not be instantiated
    }

    /**
     * Checks if the "Developer Mode" (proxied by Manage External Storage permission) is active.
     * <p>
     * On Android 11+ (API 30+), this checks {@link Environment#isExternalStorageManager()}.
     * On older versions, it returns {@code false} as the permission concept differs.
     *
     * @param context The context (unused in current implementation but kept for API consistency).
     * @return {@code true} if the user has granted the special access permission.
     */
    public static boolean hasManageExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        // This permission does not exist on versions older than Android 11 (API 30).
        // The previous implementation incorrectly checked the manifest instead of
        // performing a runtime check. Returning false is the correct behavior.
        return false;
    }

    /**
     * Shows a Toast message only if "Developer Mode" is active.
     * <p>
     * If the permission is not granted (simulating a "release" user), the message is
     * logged to Logcat (tag "DebugUIHelper") instead of being shown on screen.
     *
     * @param context The context to use for the Toast.
     * @param message The message to display or log.
     */
    public static void showToast(final Context context, final String message) {
        if (!hasManageExternalStoragePermission(context)) {
            Log.d(TAG, "Toast (release build, hidden): " + message);
            return;
        }

        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
        } else {
            Log.d(TAG, "Toast (no activity): " + message);
        }
    }
}
