package com.biglucas.agena.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.io.File;

public class StorageHelper {
    private static final String TAG = "StorageHelper";
    public static final String HISTORY_FILENAME = "history.db";
    private static final String AGENA_DIR_NAME = "AGENA";

    private StorageHelper() {
        // This is a utility class and should not be instantiated
        throw new AssertionError("This class is not meant to be instantiated.");
    }

    /**
     * Determines the optimal database path.
     * In debug builds, it tries to use external storage to survive uninstallation.
     * In release builds or if external storage is unavailable, it returns null,
     * indicating that the app's private storage should be used.
     *
     * @param context The application context.
     * @return The absolute path to the database file, or null to use private storage.
     */
    public static String getDatabasePath(Context context) {
        if (!DebugUIHelper.hasManageExternalStoragePermission(context)) {
            Log.d(TAG, "Release build - using private storage");
            return null; // Use private storage for release builds
        }

        if (!hasStoragePermission(context)) {
            Log.e(TAG, "No storage permission, falling back to private storage");
            DebugUIHelper.showToast(context, "DB: No permission - using private storage");
            return null;
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

    private static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // No runtime permissions needed before Marshmallow
    }
}
