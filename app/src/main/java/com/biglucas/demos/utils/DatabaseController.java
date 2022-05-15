package com.biglucas.demos.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

public class DatabaseController {
    final SQLiteDatabase db;
    public DatabaseController(SQLiteDatabase db) {
        this.db = db;
        db.execSQL("create table if not exists history (url text, accessed timestamp default CURRENT_TIMESTAMP);");
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
            String toAddd = String.format("=> %s %s %s", uri, timestamp, uri);
            System.out.println(toAddd);
            list.add(toAddd);
        }
        cursor.close();
        return list;
    }
}
