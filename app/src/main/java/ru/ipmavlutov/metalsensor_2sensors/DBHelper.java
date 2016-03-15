package ru.ipmavlutov.metalsensor_2sensors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private static final String LOG_TAG = "DBHelper";

    public DBHelper(Context context) {
        super(context, "DB_SENSOR", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOG_TAG, "--- onCreate database ---");
        // создаем таблицу с полями

        db.execSQL("create table Statistic ("
                + "id integer primary key autoincrement,"
                + "date string,"
                + "temperature real,"
                + "signal real,"
                + "super_signal real);");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
