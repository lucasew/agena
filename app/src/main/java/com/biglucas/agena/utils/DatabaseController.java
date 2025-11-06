package com.biglucas.agena.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
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

public class DatabaseController {
    private static final String TAG = "DatabaseController";
    final SQLiteDatabase db;

    public DatabaseController(SQLiteDatabase db) {
        this.db = db;
        db.execSQL("create table if not exists history (url text, accessed timestamp default CURRENT_TIMESTAMP);");
    }

    /**
     * Opens or creates the history database.
     * In debug builds, tries to store in Downloads/AGENA to survive uninstallation.
     * Falls back to private directory if external storage is not accessible.
     * In release builds, always uses the app's private directory.
     */
    public static SQLiteDatabase openDatabase(Context context) {
        boolean isDebug = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        Log.d(TAG, "openDatabase called - isDebug: " + isDebug + ", SDK_INT: " + Build.VERSION.SDK_INT);

        if (isDebug) {
            // Debug: Try to save in Downloads/AGENA so it survives uninstallation

            // Check if we have storage permission (Android 6-12)
            boolean hasPermission = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                hasPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "Storage permission check: " + hasPermission);
            }

            // For Android 13+ (API 33+), writing arbitrary files to Downloads is very restricted
            // Fall back to private storage for these versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.w(TAG, "Android 13+ detected, using private storage (Downloads access restricted)");
                showToast(context, "DB: Using private storage (Android 13+)");
                return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
            }

            if (!hasPermission) {
                Log.e(TAG, "No storage permission, falling back to private storage");
                Log.e(TAG, "Grant storage permission to persist database across uninstalls");
                showToast(context, "DB: No permission - using private storage");
                return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
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
                        return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
                    }
                }

                // Check if directory is writable
                boolean canWrite = agenaDir.canWrite();
                Log.d(TAG, "Directory canWrite: " + canWrite);
                if (!canWrite) {
                    Log.e(TAG, "AGENA directory not writable, falling back to private storage");
                    showToast(context, "DB: Dir not writable - using private storage");
                    return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
                }

                File dbFile = new File(agenaDir, "history.db");
                String dbPath = dbFile.getAbsolutePath();
                Log.i(TAG, "✅ Opening database at: " + dbPath);
                showToast(context, "✅ DB at: Downloads/AGENA/history.db");
                return SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            } catch (Exception e) {
                // If any error occurs (permissions, etc), fall back to private storage
                Log.e(TAG, "Error accessing Downloads directory: " + e.getMessage(), e);
                showToast(context, "DB: Error - using private storage");
                return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
            }
        } else {
            // Release: Use private directory for better security
            Log.d(TAG, "Release build - using private storage");
            return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
        }
    }

    private static void showToast(final Context context, final String message) {
        // Only show toasts in debug builds
        boolean isDebug = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!isDebug) {
            Log.d(TAG, "Toast (release build, hidden): " + message);
            return;
        }

        // Show toast on main thread
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // If not an Activity context, just log
            Log.d(TAG, "Toast (no activity): " + message);
        }
    }
    public void addHistoryEntry(Uri uri) {
        Log.d(TAG, "Saving to history: " + uri.toString());
        this.db.execSQL("insert into history (url) values (?)", new String[]{uri.toString()});
    }
    public ArrayList<String> getHistoryLines() {
        ArrayList<String> list = new ArrayList<>();
        Cursor cursor = this.db.rawQuery("select * from history order by accessed desc", null);
        Log.d(TAG, "getHistoryLines - found " + cursor.getCount() + " entries");
        while (cursor.moveToNext()) {
            String uri = cursor.getString(cursor.getColumnIndex("url"));
            String timestamp = cursor.getString(cursor.getColumnIndex("accessed"));
            String toAdd = String.format("=> %s %s %s", uri, timestamp, uri);
            Log.d(TAG, "History entry: " + toAdd);
            list.add(toAdd);
        }
        cursor.close();
        return list;
    }
}
