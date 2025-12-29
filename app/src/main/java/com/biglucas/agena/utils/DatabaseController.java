package com.biglucas.agena.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseController {
    private static final String TAG = "DatabaseController";
    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_ACCESSED = "accessed";
    private static final String HISTORY_FILENAME = "history.db";

    private static final String SQL_CREATE_HISTORY_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + " (" +
            COLUMN_URL + " TEXT, " +
            COLUMN_ACCESSED + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");";

    private static final String SQL_INSERT_HISTORY_URL =
        "INSERT INTO " + TABLE_HISTORY + " (" + COLUMN_URL + ") VALUES (?);";

    private static final String SQL_SELECT_ALL_HISTORY_ORDERED =
        "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_ACCESSED + " DESC;";

    private static final String HISTORY_LINE_FORMAT = "=> %s %s %s";
    private static final String FILENAME = "history";
    final SQLiteDatabase db;

    public DatabaseController(SQLiteDatabase db) {
        this.db = db;
        db.execSQL(SQL_CREATE_HISTORY_TABLE);
    }

    /**
     * Opens or creates the history database.
     * In debug builds, tries to store in Downloads/AGENA to survive uninstallation.
     * Falls back to private directory if external storage is not accessible.
     * In release builds, always uses the app's private directory.
     */
    public static SQLiteDatabase openDatabase(Context context) {
        // Detect debug build by checking if MANAGE_EXTERNAL_STORAGE permission exists in manifest
        // This permission is only declared in src/debug/AndroidManifest.xml
        boolean isDebug = hasManageExternalStoragePermission(context);

        Log.d(TAG, "openDatabase called - isDebug: " + isDebug + ", SDK_INT: " + Build.VERSION.SDK_INT);

        if (isDebug) {
            // Debug: Try to save in Downloads/AGENA so it survives uninstallation

            boolean hasPermission = false;

            // Android 11+ (API 30+): Check MANAGE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hasPermission = Environment.isExternalStorageManager();
                Log.d(TAG, "Android 11+ - MANAGE_EXTERNAL_STORAGE: " + hasPermission);
            }
            // Android 6-10 (API 23-29): Check WRITE_EXTERNAL_STORAGE
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "Android 6-10 - WRITE_EXTERNAL_STORAGE: " + hasPermission);
            }
            // Android < 6: No runtime permissions needed
            else {
                hasPermission = true;
                Log.d(TAG, "Android < 6 - No runtime permissions needed");
            }

            if (!hasPermission) {
                Log.e(TAG, "No storage permission, falling back to private storage");
                showToast(context, "DB: No permission - using private storage");
                return context.openOrCreateDatabase(FILENAME, Context.MODE_PRIVATE, null);
            }

            try {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File agenaDir = new File(downloadsDir, "AGENA");

                Log.d(TAG, "Downloads dir: " + downloadsDir.getAbsolutePath());
                Log.d(TAG, "AGENA dir: " + agenaDir.getAbsolutePath());
                Log.d(TAG, "AGENA dir exists: " + agenaDir.exists());

                // Ensure directory exists and is writable
                if (!agenaDir.exists()) {
                    boolean created = agenaDir.mkdirs();
                    Log.d(TAG, "mkdirs() result: " + created);
                    if (!created) {
                        Log.e(TAG, "Failed to create AGENA directory, falling back to private storage");
                        showToast(context, "DB: Failed to create dir - using private storage");
                        return context.openOrCreateDatabase(FILENAME, Context.MODE_PRIVATE, null);
                    }
                }

                // Check if directory is writable
                boolean canWrite = agenaDir.canWrite();
                Log.d(TAG, "Directory canWrite: " + canWrite);
                if (!canWrite) {
                    Log.e(TAG, "AGENA directory not writable, falling back to private storage");
                    showToast(context, "DB: Dir not writable - using private storage");
                    return context.openOrCreateDatabase(FILENAME, Context.MODE_PRIVATE, null);
                }

                File dbFile = new File(agenaDir, HISTORY_FILENAME);
                String dbPath = dbFile.getAbsolutePath();
                Log.i(TAG, "✅ Opening database at: " + dbPath);
                showToast(context, "✅ DB at: Downloads/AGENA/" + HISTORY_FILENAME);
                return SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            } catch (Exception e) {
                // If any error occurs (permissions, etc), fall back to private storage
                Log.e(TAG, "Error accessing Downloads directory: " + e.getMessage(), e);
                showToast(context, "DB: Error - using private storage");
                return context.openOrCreateDatabase(FILENAME, Context.MODE_PRIVATE, null);
            }
        } else {
            // Release: Use private directory for better security
            Log.d(TAG, "Release build - using private storage");
            return context.openOrCreateDatabase(FILENAME, Context.MODE_PRIVATE, null);
        }
    }

    private static boolean hasManageExternalStoragePermission(Context context) {
        try {
            String[] permissions = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                .requestedPermissions;

            if (permissions != null) {
                for (String permission : permissions) {
                    if ("android.permission.MANAGE_EXTERNAL_STORAGE".equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
        }
        return false;
    }

    private static void showToast(final Context context, final String message) {
        // Only show toasts in debug builds
        boolean isDebug = hasManageExternalStoragePermission(context);
        if (!isDebug) {
            Log.d(TAG, "Toast (release build, hidden): " + message);
            return;
        }

        // Show toast on main thread
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
        } else {
            // If not an Activity context, just log
            Log.d(TAG, "Toast (no activity): " + message);
        }
    }
    public void addHistoryEntry(Uri uri) {
        Log.d(TAG, "Saving to history: " + uri.toString());
        this.db.execSQL(SQL_INSERT_HISTORY_URL, new String[]{uri.toString()});
    }
    public List<String> getHistoryLines() {
        ArrayList<String> list = new ArrayList<>();
        Cursor cursor = this.db.rawQuery(SQL_SELECT_ALL_HISTORY_ORDERED, null);
        Log.d(TAG, "getHistoryLines - found " + cursor.getCount() + " entries");
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String uri = cursor.getString(cursor.getColumnIndex(COLUMN_URL));
            @SuppressLint("Range") String timestamp = cursor.getString(cursor.getColumnIndex(COLUMN_ACCESSED));
            String toAdd = String.format(HISTORY_LINE_FORMAT, uri, timestamp, uri);
            Log.d(TAG, "History entry: " + toAdd);
            list.add(toAdd);
        }
        cursor.close();
        return list;
    }
}
