package com.biglucas.agena.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;

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
     * In debug builds, the database is stored in Downloads/AGENA to survive uninstallation.
     * In release builds, the database is stored in the app's private directory.
     */
    public static SQLiteDatabase openDatabase(Context context) {
        boolean isDebug = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        if (isDebug) {
            // Debug: Save in Downloads/AGENA so it survives uninstallation
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File agenaDir = new File(downloadsDir, "AGENA");
            if (!agenaDir.exists()) {
                agenaDir.mkdirs();
            }
            File dbFile = new File(agenaDir, "history.db");
            return SQLiteDatabase.openOrCreateDatabase(dbFile, null);
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
