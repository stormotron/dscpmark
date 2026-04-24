package com.netstorm.dscpmark;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "dscpmark.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "dscp_rules";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (package_name TEXT PRIMARY KEY, dscp_mark INTEGER, uid INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveRules(Map<String, AppItem> selectedApps) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_NAME);
            for (AppItem item : selectedApps.values()) {
                if (item.isSelected) {
                    ContentValues cv = new ContentValues();
                    cv.put("package_name", item.packageName);
                    cv.put("dscp_mark", item.dscpMark);
                    cv.put("uid", item.uid);
                    db.insert(TABLE_NAME, null, cv);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public Map<String, AppItem> getSavedRules() {
        Map<String, AppItem> rules = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            do {
                AppItem item = new AppItem(
                        cursor.getString(0), "", null, cursor.getInt(2));
                item.dscpMark = cursor.getInt(1);
                item.isSelected = true;
                rules.put(item.packageName, item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return rules;
    }
}