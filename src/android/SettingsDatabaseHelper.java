package org.apache.cordova.stepist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SettingsDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "settings.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "user_settings";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_VOICE_ALERT = "voice_alert";
    public static final String COLUMN_REMINDER_FREQ = "reminder_frequency";
    public static final String COLUMN_SPEED_LIMIT_MAX = "speed_limit_max";
    public static final String COLUMN_SPEED_LIMIT_MIN = "speed_limit_min";
    public static final String COLUMN_VOICE_OPERATOR = "voice_operator";
    public static final String COLUMN_LANGUAGE = "language";
    public static final String COLUMN_LANGUAGE_ISO = "language_iso";
    public static final String COLUMN_LANGUAGE_RATE = "language_rate";
    public static final String COLUMN_LANGUAGE_PITCH = "language_pitch";

    public SettingsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_VOICE_ALERT + " INTEGER, " + // 0 veya 1
                COLUMN_REMINDER_FREQ + " TEXT, " +
                COLUMN_SPEED_LIMIT_MAX + " REAL, " +
                COLUMN_SPEED_LIMIT_MIN + " REAL, " +
                COLUMN_VOICE_OPERATOR + " TEXT, " +
                COLUMN_LANGUAGE + " TEXT, " +
                COLUMN_LANGUAGE_ISO + " TEXT, "+
                COLUMN_LANGUAGE_RATE + " REAL, " +
                COLUMN_LANGUAGE_PITCH + " REAL " +

                ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void updateVoiceAlert(boolean enabled) {
        updateField(SettingsDatabaseHelper.COLUMN_VOICE_ALERT, enabled ? 1 : 0);
    }

    public void updateReminderFrequency(String frequency) {
        updateField(SettingsDatabaseHelper.COLUMN_REMINDER_FREQ, frequency);
    }

    public void updateSpeedLimitMax(double maxSpeed) {
        updateField(SettingsDatabaseHelper.COLUMN_SPEED_LIMIT_MAX, maxSpeed);
    }

    public void updateSpeedLimitMin(double minSpeed) {
        updateField(SettingsDatabaseHelper.COLUMN_SPEED_LIMIT_MIN, minSpeed);
    }

    public void updateVoiceOperator(String operatorName) {
        updateField(SettingsDatabaseHelper.COLUMN_VOICE_OPERATOR, operatorName);
    }

    public void updateLanguage(String language) {
        updateField(SettingsDatabaseHelper.COLUMN_LANGUAGE, language);
    }

    public void updateLanguageIso(String isoCode) {
        updateField(SettingsDatabaseHelper.COLUMN_LANGUAGE_ISO, isoCode);
    }

    public void updateLanguageRate(double rate) {
        updateField(SettingsDatabaseHelper.COLUMN_LANGUAGE_RATE, rate);
    }

    public void updateLanguagePitch(double pitch) {
        updateField(SettingsDatabaseHelper.COLUMN_LANGUAGE_PITCH, pitch);
    }

    public void updateField(String columnName, Object value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (value instanceof String) {
            values.put(columnName, (String) value);
        } else if (value instanceof Integer) {
            values.put(columnName, (Integer) value);
        } else if (value instanceof Double) {
            values.put(columnName, (Double) value);
        }

        db.update(SettingsDatabaseHelper.TABLE_NAME, values, SettingsDatabaseHelper.COLUMN_ID + " = ?", new String[]{"1"});
        db.close();
    }
    public String getField(String columnName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{columnName}, COLUMN_ID + " = ?", new String[]{"1"}, null, null, null);
        String result = "";
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return result;
    }

}