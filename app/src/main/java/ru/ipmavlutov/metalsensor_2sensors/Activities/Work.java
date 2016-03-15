package ru.ipmavlutov.metalsensor_2sensors.Activities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import ru.ipmavlutov.metalsensor_2sensors.BluetoothResponseHandler;
import ru.ipmavlutov.metalsensor_2sensors.DBHelper;
import ru.ipmavlutov.metalsensor_2sensors.DeviceConnector;
import ru.ipmavlutov.metalsensor_2sensors.DeviceData;
import ru.ipmavlutov.metalsensor_2sensors.DeviceListActivity;
import ru.ipmavlutov.metalsensor_2sensors.MyTimerTask;
import ru.ipmavlutov.metalsensor_2sensors.R;

public class Work extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int PREFERENCE_MODE_PRIVATE = 0;



    private static final String TAG = "WORK";
    public static double Z;
    public String MAC_ADDRESS;

    BluetoothAdapter btAdapter;
    private DeviceConnector connector;
    BluetoothResponseHandler mHandler;
    public String MSG_NOT_CONNECTED;
    public String MSG_CONNECTING;
    public String MSG_CONNECTED;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private TextView current_value;
    public static DBHelper dbHelper;
    public static double super_signal;
    public static double signal;
    public static int temperature;
    private Timer my_tm;
    private TimerTask my_tt;


    public static DBHelper getDbHelper() {
        return dbHelper;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent graphs = new Intent(getBaseContext(), GraphActivity.class);
                startActivity(graphs);
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        /***********/
        //textView=(TextView)findViewById(R.id.absolute_label);
        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected);
        MSG_CONNECTING = getString(R.string.msg_connecting);
        MSG_CONNECTED = getString(R.string.msg_connected);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            MAC_ADDRESS = bundle.getString(EXTRA_DEVICE_ADDRESS);
            try {
                BluetoothDevice device = btAdapter.getRemoteDevice(MAC_ADDRESS);
                if (btAdapter.isEnabled() && (connector == null))
                    setupConnector(device);
            } catch (Exception ex) {
                Toast.makeText(Work.this, "Невозможно подключиться", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        if (sharedPreferences.contains("Z")) {
            Z = sharedPreferences.getFloat("Z", (float) Z);
        } else {
            Z = 96.0;
        }
        current_value = (TextView) findViewById(R.id.current_value_value);
        current_value.setText(String.valueOf(Z));
        Button abs_btn = (Button) findViewById(R.id.set_absolute_btn);
        abs_btn.setOnClickListener(abs_click);

        dbHelper = new DBHelper(this);
        my_tm = new Timer();
        my_tt = new MyTimerTask();
    }

    View.OnClickListener abs_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sharedPreferences = getPreferences(PREFERENCE_MODE_PRIVATE);
            sharedPreferencesEditor = sharedPreferences.edit();
            switch (v.getId()) {
                case R.id.set_absolute_btn:
                    EditText editText = (EditText) findViewById(R.id.editText);
                    String p0_value = editText.getText().toString();
                    if (p0_value.isEmpty()) {
                        Z = sharedPreferences.getFloat("Z", (float) Z);
                        current_value.setText(String.valueOf(Z));

                    } else {
                        Z = Double.parseDouble(p0_value);
                        current_value.setText(String.valueOf(Z));
                        sharedPreferencesEditor.putFloat("Z", (float) Z);
                        sharedPreferencesEditor.apply();
                    }
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        stopConnection();

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopConnection();
        Log.d(TAG, "onDestroy");
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_connect) {
            stopConnection();
            Intent serverIntent = new Intent(getBaseContext(), DeviceListActivity.class);
            startActivity(serverIntent);
        } else if (id == R.id.nav_graph) {
            Intent graph = new Intent(getBaseContext(), GraphActivity.class);
            startActivity(graph);

        } else if (id == R.id.nav_export) {
            new ExportDBtoCSV().execute();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    MAC_ADDRESS = address;
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    if (btAdapter.isEnabled() && (connector == null))
                        setupConnector(device);
                }
                break;

        }
    }

    /********************************************************************************************/
    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }

    boolean isAdapterReady() {
        return (btAdapter != null) &&
                (btAdapter.isEnabled());
    }

    private boolean isConnected() {
        return (connector != null) && (connector.getState() == DeviceConnector.STATE_CONNECTED);
    }

    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = getString(R.string.empty_device_name);
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            connector.connect();
        } catch (IllegalArgumentException | IOException ignored) {

        }
    }

    public void setDeviceName(String deviceName) {
        getSupportActionBar().setSubtitle(deviceName);
    }

    public void UpDateBD(int get_temperature, double get_signal, double get_super_signal) {
        temperature = get_temperature;
        signal = get_signal;
        super_signal = get_super_signal;
    }

    public void myTimer(boolean first_start) {
        if (first_start)
            my_tm.schedule(my_tt, 5000, (15 * 60 * 1000));
    }

    public void myTimer_off() {
        my_tt.cancel();
        my_tm.cancel();
    }

    public class ExportDBtoCSV extends AsyncTask<String, Void, Boolean> {
        DBHelper dbhelper = new DBHelper(Work.this);
        private final ProgressDialog dialog = new ProgressDialog(Work.this);

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Exporting database...");
            this.dialog.show();
        }

        protected Boolean doInBackground(final String... args) {
            //File dbFile = getDatabasePath("database_name");
            //AABDatabaseManager dbhelper = new AABDatabaseManager(getApplicationContext());
            //System.out.println(dbFile);  // displays the data base path in your logcat
            File exportDir = new File(Environment.getExternalStorageDirectory(), "");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File file = new File(exportDir, "exportDB.csv");
            try {
                if (file.createNewFile()) {
                    System.out.println("File is created!");
                    System.out.println("myDB.csv " + file.getAbsolutePath());
                } else {
                    System.out.println("File already exists.");
                }
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                SQLiteDatabase db = dbhelper.getWritableDatabase();
                Cursor curCSV = db.rawQuery("select * from Statistic", null);
                csvWrite.writeNext(curCSV.getColumnNames());
                while (curCSV.moveToNext()) {
                    String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4)};
         /*curCSV.getString(3),curCSV.getString(4)};*/
                    csvWrite.writeNext(arrStr);
                }
                csvWrite.close();
                curCSV.close();
        /*String data="";
        data=readSavedData();
        data= data.replace(",", ";");
        writeData(data);*/
                return true;
            } catch (IOException e) {
                Log.e("MainActivity", e.getMessage(), e);
                return false;
            }
        }

        protected void onPostExecute(final Boolean success) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            if (success) {
                Toast.makeText(getApplicationContext(), "Export succeed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
