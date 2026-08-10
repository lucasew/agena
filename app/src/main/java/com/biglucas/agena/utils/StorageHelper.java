package com.biglucas.agena.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.biglucas.agena.protocol.gemini.GeminiSpec;

import java.io.File;

public class StorageHelper {
    private static final String TAG = "StorageHelper";
    public static final String HISTORY_FILENAME = "history.db";

    private StorageHelper() {
        // This is a utility class and should not be instantiated
    }

    /**
     * Determines the optimal database path based on the build configuration and permissions.
     * <p>
     * <b>Strategy:</b>
     * <ul>
     *     <li><b>Developer Mode (External Storage):</b> If {@link DebugUIHelper#hasManageExternalStoragePermission(Context)} returns true,
     *         it attempts to use a file in the public 'Downloads/AGENA' directory. This allows the database to persist across app uninstallations,
     *         which is critical for debugging and development continuity.</li>
     *     <li><b>Production Mode (Private Storage):</b> If the permission is missing (standard release builds), it returns {@code null},
     *         signaling the caller to use the app's secure private storage.</li>
     * </ul>
     *
     * @param context The application context.
     * @return The absolute path to the external database file, or {@code null} to indicate private storage should be used.
     */
    public static String getDatabasePath(Context context) {
        if (!DebugUIHelper.hasManageExternalStoragePermission(context)) {
            Log.d(TAG, "Release build - using private storage");
            return null; // Use private storage for release builds
        }

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File agenaDir = new File(downloadsDir, GeminiSpec.DOWNLOAD_DIRECTORY_NAME);

            if (!agenaDir.exists()) {
                if (!agenaDir.mkdirs()) {
                    return fallBackToPrivateStorage(
                            context,
                            "Failed to create AGENA directory, falling back to private storage",
                            "DB: Failed to create dir - using private storage");
                }
            }

            if (!agenaDir.canWrite()) {
                return fallBackToPrivateStorage(
                        context,
                        "AGENA directory not writable, falling back to private storage",
                        "DB: Dir not writable - using private storage");
            }

            File dbFile = new File(agenaDir, HISTORY_FILENAME);
            String dbPath = dbFile.getAbsolutePath();
            Log.i(TAG, "✅ Using database at: " + dbPath);
            DebugUIHelper.showToast(
                    context,
                    "✅ DB at: Downloads/" + GeminiSpec.DOWNLOAD_DIRECTORY_NAME + "/" + HISTORY_FILENAME);
            return dbPath;
        } catch (Exception e) {
            return fallBackToPrivateStorage(
                    context,
                    "Error accessing Downloads directory: " + e.getMessage(),
                    "DB: Error - using private storage",
                    e);
        }
    }

    /**
     * Logs a path-resolution failure, shows a debug toast, and signals private storage.
     *
     * @return always {@code null} so callers can {@code return} the helper directly
     */
    private static String fallBackToPrivateStorage(Context context, String logMessage, String toastMessage) {
        return fallBackToPrivateStorage(context, logMessage, toastMessage, null);
    }

    private static String fallBackToPrivateStorage(
            Context context, String logMessage, String toastMessage, Throwable cause) {
        if (cause != null) {
            ErrorReporter.reportError(TAG, logMessage, cause);
        } else {
            ErrorReporter.reportError(TAG, logMessage);
        }
        DebugUIHelper.showToast(context, toastMessage);
        return null;
    }
}
