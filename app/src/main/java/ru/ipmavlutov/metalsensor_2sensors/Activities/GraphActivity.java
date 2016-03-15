package ru.ipmavlutov.metalsensor_2sensors.Activities;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import ru.ipmavlutov.metalsensor_2sensors.DBHelper;
import ru.ipmavlutov.metalsensor_2sensors.Fragments.DatePickerFragment;
import ru.ipmavlutov.metalsensor_2sensors.Fragments.DatePickerFragment.DatePickerFragmentListener;
import ru.ipmavlutov.metalsensor_2sensors.R;


public class GraphActivity extends AppCompatActivity
        implements DatePickerFragmentListener {

    private DatePickerFragment fragment;
    private DatePickerFragment fragment2;

    private LineChart lineChart;
    private LineData lineData;
    private DBHelper dbHelper;
    private int day;
    private int month;
    private int year;
    private int day2;
    private int month2;
    private int year2;
    private TextView label_date;
    private TextView label_date2;
    private String[] dateArr;
    private double[] tempArr;
    private double[] super_signal;
    private int size;
    private double[] signalArr;

    public String getStartDateQueryDate() {
        return startDateQueryDate;
    }

    public void setStartDateQueryDate(String startDateQueryDate) {
        this.startDateQueryDate = startDateQueryDate;
    }

    public String getEndDateQueryDate() {
        return endDateQueryDate;
    }

    public void setEndDateQueryDate(String endDateQueryDate) {
        this.endDateQueryDate = endDateQueryDate;
    }

    private String startDateQueryDate;
    private String endDateQueryDate;
    private MyThread MT;
    private double[] super_signalArr;
    private Button btn_test;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        lineChart = (LineChart) findViewById(R.id.line_chart);
        lineData = new LineData();
        dbHelper = new DBHelper(this);
        dbHelper.getReadableDatabase();
        Button btn_dateDialog = (Button) findViewById(R.id.btn_date_picker);
        label_date = (TextView) findViewById(R.id.textView3);
        label_date2 = (TextView) findViewById(R.id.textView4);
        fragment = DatePickerFragment.newInstance(this);
        fragment2 = DatePickerFragment.newInstance(this);
        btn_dateDialog.setOnClickListener(lister);
        MT = new MyThread();
        btn_test=(Button)findViewById(R.id.test);
        btn_test.setOnClickListener(lister);


    }

    private void setData(String[] dateArr, double[] signalArr, double[] tempArr,
                         double[] super_signal, int size) {
        this.dateArr = dateArr;
        this.signalArr = signalArr;
        this.tempArr = tempArr;
        this.super_signal = super_signal;
        this.size = size;

        ArrayList<String> dateVals = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            dateVals.add(dateArr[i]);
        }

        ArrayList<Entry> signalVals = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            signalVals.add(new Entry((float) signalArr[i], i));
        }
        ArrayList<Entry> super_signalVals = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            super_signalVals.add(new Entry((float) super_signal[i], i));
        }
        ArrayList<Entry> temperatureVals = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            temperatureVals.add(new Entry((float) tempArr[i], i));
        }

        LineDataSet set1 = new LineDataSet(signalVals, "Частицы");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setCircleColor(Color.WHITE);
        set1.setLineWidth(2f);
        set1.setCircleRadius(3f);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);


        LineDataSet set2 = new LineDataSet(temperatureVals, "Температура");
        set2.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set2.setColor(Color.RED);
        set2.setCircleColor(Color.WHITE);
        set2.setLineWidth(2f);
        set2.setCircleRadius(3f);
        set2.setFillAlpha(65);
        set2.setFillColor(Color.RED);
        set2.setDrawCircleHole(false);
        set2.setHighLightColor(Color.rgb(244, 117, 117));
        set2.setDrawCircleHole(false);

        LineDataSet set3 = new LineDataSet(super_signalVals, "Частиц+");
        set3.setAxisDependency(YAxis.AxisDependency.LEFT);
        set3.setColor(Color.GREEN);
        set3.setCircleColor(Color.WHITE);
        set3.setLineWidth(2f);
        set3.setCircleRadius(3f);
        set3.setFillAlpha(65);
        set3.setFillColor(ColorTemplate.getHoloBlue());
        set3.setHighLightColor(Color.rgb(244, 117, 117));
        set3.setDrawCircleHole(false);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        dataSets.add(set2);
        dataSets.add(set3);


        //ArrayList<> xValsArrayList=new ArrayList<>();

        // create a data object with the datasets
        lineData = new LineData(dateVals, dataSets);
        lineData.setValueTextColor(Color.BLACK);
        lineData.setValueTextSize(9f);

        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setDescription("Показание данных");
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setPinchZoom(true);

        // set data
        lineChart.setData(lineData);
    }


    private class MyThread implements Runnable {
        public void run() {

            SQLiteDatabase db = dbHelper.getReadableDatabase();
           Cursor c = db.rawQuery("SELECT * FROM Statistic WHERE date >= Datetime('"+getStartDateQueryDate()+" 00:00:00') AND date <= Datetime('"+getEndDateQueryDate()+" 23:59:59')",null);
            Log.d("TAG", "CURSOR SQL");
            if (c.moveToFirst()) {
                // определяем номера столбцов по имени в выборке
                dateArr = new String[c.getCount() + 1];
                tempArr = new double[c.getCount() + 1];
                signalArr = new double[c.getCount() + 1];
                super_signalArr = new double[c.getCount() + 1];
                AtomicInteger dateColIndex = new AtomicInteger(c.getColumnIndex("date"));
                AtomicInteger signalColIndex = new AtomicInteger(c.getColumnIndex("signal"));
                AtomicInteger tempColIndex = new AtomicInteger(c.getColumnIndex("temperature"));
                AtomicInteger super_signalColIndex = new AtomicInteger(c.getColumnIndex("super_signal"));
                for (int i = 0; i < c.getCount(); i++) {
                    dateArr[i] = c.getString(dateColIndex.get());
                    signalArr[i] = c.getDouble(signalColIndex.get());
                    tempArr[i] = c.getInt(tempColIndex.get());
                    super_signalArr[i] = c.getInt(super_signalColIndex.get());
                    Log.d("TAG", "i:= " + i + "D: " + dateArr[i] + " S: " + signalArr[i] + "SS: " + super_signalArr[i] + " T: " + tempArr[i]);
                    c.moveToNext();
                }
                //setData(dateArr, signalArr, tempArr,c.getCount());
            } else {
                c.close();
            }

            setData(dateArr, signalArr, tempArr, super_signalArr, c.getCount());
            lineChart.invalidate();
        }
    }

    private void setYear2(int year2) {
        this.year2 = year2;
    }

    private void setMonth2(int month2) {
        this.month2 = month2 + 1;
    }

    private void setDay2(int day2) {
        this.day2 = day2;
    }

    private void setDay(int day) {
        this.day = day;
    }

    private void setMonth(int month) {
        this.month = month + 1;
    }

    private void setYear(int year) {
        this.year = year;
    }

    private View.OnClickListener lister = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_date_picker:
                    fragment2.show(getSupportFragmentManager(), "DatePicker2");
                    fragment.show(getSupportFragmentManager(), "DatePicker");
                    break;
                case R.id.test:
                    DBHelper db = new DBHelper(GraphActivity.this);
                    SQLiteDatabase sqbd =db.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    for (int i=1;i<10;i++){
                        cv.put("date", "2016-01-0"+i+" 0"+i+":0"+i);
                        cv.put("temperature", 20+i%2);
                        cv.put("signal", 13.37);
                        cv.put("super_signal",1488.0);

                        long rowID = sqbd.insert("Statistic", null, cv);
                        Log.d("TEST SQL DATA", "row inserted, ID = " + rowID + " " + cv);
                    }
                    db.close();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void DataSet(Date date, String tag) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if (tag.equals(fragment.getTag())) {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            setDay(c.get(Calendar.DAY_OF_MONTH));
            setMonth(c.get(Calendar.MONTH));
            setYear(c.get(Calendar.YEAR));
            label_date.setText(String.format("С: %d/%d/%d",
                    day, month, year));
            setStartDateQueryDate(sdf.format(date));
            Toast.makeText(GraphActivity.this,
                    "Первая дата С: " + day + "/" + month + "/" + year,
                    Toast.LENGTH_SHORT).show();
            Log.d("TAG", "Start=" + startDateQueryDate);
        }
        if (tag.equals(fragment2.getTag())) {
            Calendar c = Calendar.getInstance();
            c.setTime(date);

            setDay2(c.get(Calendar.DAY_OF_MONTH));
            setMonth2(c.get(Calendar.MONTH));
            setYear2(c.get(Calendar.YEAR));
            setEndDateQueryDate(sdf.format(date));
            label_date2.setText(String.format("По: %d/%d/%d",
                    day2, month2, year2));
            Toast.makeText(GraphActivity.this,
                    "Вторая дата По:  " + day2 + "/" + month2 + "/" + year2,
                    Toast.LENGTH_SHORT).show();
            Log.d("TAG", "END=" + endDateQueryDate);
            if (endDateQueryDate != null) {
                Runnable r = new MyThread();
                new Thread(r).run();
            }

        }

    }

}

