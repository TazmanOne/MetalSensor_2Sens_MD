package ru.ipmavlutov.metalsensor_2sensors.Activities;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.ipmavlutov.metalsensor_2sensors.DeviceConnector;
import ru.ipmavlutov.metalsensor_2sensors.DeviceListActivity;
import ru.ipmavlutov.metalsensor_2sensors.R;

public class Welcome extends AppCompatActivity {
    private static final String TAG = "WELCOME DEBAGING";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private DeviceConnector connector;
    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Button btn_search = (Button) findViewById(R.id.button);
        btn_search.setOnClickListener(start_search);
        Button btn_graph = (Button) findViewById(R.id.button2);

        btn_graph.setOnClickListener(start_graph);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        TextView tv = (TextView) findViewById(R.id.welcome);
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onSTOP");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
    View.OnClickListener start_graph=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i =new Intent(getBaseContext(),GraphActivity.class);
            startActivity(i);
        }
    };
    View.OnClickListener start_search = new View.OnClickListener() {


        @Override
        public void onClick(View v) {
            if (btAdapter.isEnabled()) {
                Intent serverIntent = new Intent(getBaseContext(), DeviceListActivity.class);
                startActivity(serverIntent);
                // startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            } else {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableIntent);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Intent it = new Intent(getBaseContext(), Work.class);
                    it.putExtra(EXTRA_DEVICE_ADDRESS, address);
                    startActivity(it);
                }
                break;
            default:
                break;

        }
    }
}
