package org.apache.cordova.stepist;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StepDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "steps.db";
    private static final int DB_VERSION = 1;

    public StepDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE  IF NOT EXISTS  steps (id INTEGER PRIMARY KEY AUTOINCREMENT, count INTEGER, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void insertStep(int count) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("CREATE TABLE  IF NOT EXISTS  steps (id INTEGER PRIMARY KEY AUTOINCREMENT, count INTEGER, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

        ContentValues values = new ContentValues();
        values.put("count", count);
        db.insert("steps", null, values);
        db.close();
    }
}