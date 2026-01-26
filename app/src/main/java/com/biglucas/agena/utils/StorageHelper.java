package com.biglucas.agena.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;

public class StorageHelper {
    private static final String TAG = "StorageHelper";
    public static final String HISTORY_FILENAME = "history.db";
    private static final String AGENA_DIR_NAME = "AGENA";

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
            File agenaDir = new File(downloadsDir, AGENA_DIR_NAME);

            if (!agenaDir.exists()) {
                if (!agenaDir.mkdirs()) {
                    Log.e(TAG, "Failed to create AGENA directory, falling back to private storage");
                    DebugUIHelper.showToast(context, "DB: Failed to create dir - using private storage");
                    return null;
                }
            }

            if (!agenaDir.canWrite()) {
                Log.e(TAG, "AGENA directory not writable, falling back to private storage");
                DebugUIHelper.showToast(context, "DB: Dir not writable - using private storage");
                return null;
            }

            File dbFile = new File(agenaDir, HISTORY_FILENAME);
            String dbPath = dbFile.getAbsolutePath();
            Log.i(TAG, "✅ Using database at: " + dbPath);
            DebugUIHelper.showToast(context, "✅ DB at: Downloads/AGENA/" + HISTORY_FILENAME);
            return dbPath;
        } catch (Exception e) {
            Log.e(TAG, "Error accessing Downloads directory: " + e.getMessage(), e);
            DebugUIHelper.showToast(context, "DB: Error - using private storage");
            return null; // Fallback to private storage on any error
        }
    }
}
