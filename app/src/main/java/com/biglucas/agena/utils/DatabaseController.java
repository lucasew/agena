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

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;

public class DatabaseController {
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

        if (isDebug) {
            // Debug: Try to save in Downloads/AGENA so it survives uninstallation

            // Check if we have storage permission (Android 6-12)
            boolean hasPermission = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                hasPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            }

            // For Android 13+ (API 33+), writing arbitrary files to Downloads is very restricted
            // Fall back to private storage for these versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                System.out.println("Android 13+ detected, using private storage (Downloads access restricted)");
                return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
            }

            if (!hasPermission) {
                System.err.println("No storage permission, falling back to private storage");
                System.err.println("Grant storage permission to persist database across uninstalls");
                return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
            }

            try {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File agenaDir = new File(downloadsDir, "AGENA");

                // Ensure directory exists and is writable
                if (!agenaDir.exists()) {
                    if (!agenaDir.mkdirs()) {
                        System.err.println("Failed to create AGENA directory, falling back to private storage");
                        return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
                    }
                }

                // Check if directory is writable
                if (!agenaDir.canWrite()) {
                    System.err.println("AGENA directory not writable, falling back to private storage");
                    return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
                }

                File dbFile = new File(agenaDir, "history.db");
                System.out.println("Opening database at: " + dbFile.getAbsolutePath());
                return SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            } catch (Exception e) {
                // If any error occurs (permissions, etc), fall back to private storage
                System.err.println("Error accessing Downloads directory: " + e.getMessage());
                e.printStackTrace();
                return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
            }
        } else {
            // Release: Use private directory for better security
            return context.openOrCreateDatabase("history", Context.MODE_PRIVATE, null);
        }
    }
    public void addHistoryEntry(Uri uri) {
        System.out.printf("Saving '%s' to historic\n", uri.toString());
        this.db.execSQL("insert into history (url) values (?)", new String[]{uri.toString()});
    }
    public ArrayList<String> getHistoryLines() {
        ArrayList<String> list = new ArrayList<>();
        Cursor cursor = this.db.rawQuery("select * from history order by accessed desc", null);
        while (cursor.moveToNext()) {
            String uri = cursor.getString(cursor.getColumnIndex("url"));
            String timestamp = cursor.getString(cursor.getColumnIndex("accessed"));
            String toAdd = String.format("=> %s %s %s", uri, timestamp, uri);
            System.out.println(toAdd);
            list.add(toAdd);
        }
        cursor.close();
        return list;
    }
}
