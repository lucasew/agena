package com.biglucas.agena.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseController {
    private static final String TAG = "DatabaseController";
    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_ACCESSED = "accessed";

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
    private static final String PRIVATE_HISTORY_FILENAME = "history";
    final SQLiteDatabase db;

    public DatabaseController(SQLiteDatabase db) {
        this.db = db;
        db.execSQL(SQL_CREATE_HISTORY_TABLE);
    }

    /**
     * Opens or creates the history database.
     * It uses StorageHelper to determine the best location.
     */
    public static SQLiteDatabase openDatabase(Context context) {
        String dbPath = StorageHelper.getDatabasePath(context);

        if (dbPath != null) {
            try {
                return SQLiteDatabase.openOrCreateDatabase(new File(dbPath), null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open external database, falling back to private storage", e);
                // Fallback to private storage if external fails for any reason
            }
        }

        // Default to private storage
        return context.openOrCreateDatabase(PRIVATE_HISTORY_FILENAME, Context.MODE_PRIVATE, null);
    }

    public void addHistoryEntry(Uri uri) {
        Log.d(TAG, "Saving to history: " + uri.toString());
        this.db.execSQL(SQL_INSERT_HISTORY_URL, new String[]{uri.toString()});
    }

    public List<String> getHistoryLines() {
        ArrayList<String> list = new ArrayList<>();
        Cursor cursor = this.db.rawQuery(SQL_SELECT_ALL_HISTORY_ORDERED, null);
        Log.d(TAG, "getHistoryLines - found " + cursor.getCount() + " entries");
        int urlColumnIndex = cursor.getColumnIndex(COLUMN_URL);
        int accessedColumnIndex = cursor.getColumnIndex(COLUMN_ACCESSED);
        while (cursor.moveToNext()) {
            String uri = cursor.getString(urlColumnIndex);
            String timestamp = cursor.getString(accessedColumnIndex);
            String toAdd = String.format(HISTORY_LINE_FORMAT, uri, timestamp, uri);
            Log.d(TAG, "History entry: " + toAdd);
            list.add(toAdd);
        }
        cursor.close();
        return list;
    }
}
