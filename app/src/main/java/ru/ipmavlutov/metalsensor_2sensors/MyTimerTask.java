package ru.ipmavlutov.metalsensor_2sensors;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimerTask;

import ru.ipmavlutov.metalsensor_2sensors.Activities.Work;

public class MyTimerTask extends TimerTask {

    private static final String LOG_TAG = "TimerTask_DB";

    @Override
    public void run() {
        SQLiteOpenHelper dbHelper = Work.getDbHelper();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(LOG_TAG, "--- Insert in my_table: ---");
        // подготовим данные для вставки в виде пар: наименование столбца - значение
        ContentValues cv = new ContentValues();
        cv.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
        cv.put("temperature", Work.temperature);
        cv.put("signal", Work.signal);
        cv.put("super_signal", Work.super_signal);
        // вставляем запись и получаем ее ID
        long rowID = db.insert("Statistic", null, cv);
        Log.d(LOG_TAG, "row inserted, ID = " + rowID + " " + cv);
        db.close();

    }

}
 
