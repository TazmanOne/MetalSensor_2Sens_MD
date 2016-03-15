package ru.ipmavlutov.metalsensor_2sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class DeviceConnector {
    private static final String TAG = "DeviceConnector";
    private static final boolean D = false;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device


    private int mState;

    private final BluetoothAdapter btAdapter;
    private final BluetoothDevice connectedDevice;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private final Handler mHandler;
    private final String deviceName;

    // ==========================================================================
    public DeviceConnector(DeviceData deviceData, Handler handler) {
        mHandler = handler;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = btAdapter.getRemoteDevice(deviceData.getAddress());
        deviceName = (deviceData.getName() == null) ? deviceData.getAddress() : deviceData.getName();
        mState = STATE_NONE;
    }


    /**
     * Запрос на соединение с устойством
     */
    public synchronized void connect() throws IOException {
        if (D) Log.d(TAG, "connect to: " + connectedDevice);

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                if (D) Log.d(TAG, "cancel mConnectThread");
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(connectedDevice);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    // ==========================================================================

    /**
     * Завершение соединения
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }
    // ==========================================================================


    /**
     * Установка внутреннего состояния устройства
     *
     * @param state - состояние
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(BluetoothResponseHandler.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    // ==========================================================================


    /**
     * Получение состояния устройства
     */
    public synchronized int getState() {
        return mState;
    }
    // ==========================================================================


    public synchronized void connected(BluetoothSocket socket) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_CONNECTED);

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BluetoothResponseHandler.MESSAGE_DEVICE_NAME, deviceName);
        mHandler.sendMessage(msg);

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

    }
    // ==========================================================================

    // ==========================================================================


    private void connectionFailed() {
        if (D) Log.d(TAG, "connectionFailed");

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothResponseHandler.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }
    // ==========================================================================


    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothResponseHandler.MESSAGE_TOAST, deviceName);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);


    }
    // ==========================================================================


    /**
     * Класс потока для соединения с BT-устройством
     */
    // ==========================================================================
    private class ConnectThread extends Thread {
        private static final String TAG = "ConnectThread";
        private static final boolean D = false;

        private final BluetoothSocket mmSocket;
        private final UUID uuids = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        public ConnectThread(BluetoothDevice device) throws IOException {
            if (D) Log.d(TAG, "create ConnectThread");
            mmSocket = device.createRfcommSocketToServiceRecord(uuids);
        }
        // ==========================================================================

        /**
         * Основной рабочий метод для соединения с устройством.
         * При успешном соединении передаёт управление другому потоку
         */
        public void run() {
            if (D) Log.d(TAG, "ConnectThread run");
            btAdapter.cancelDiscovery();
            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to connect to device, socket isn't created");
                connectionFailed();
                return;
            }

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                btAdapter.cancelDiscovery();
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    if (D) Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (DeviceConnector.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket);
        }
        // ==========================================================================


        /**
         * Отмена соединения
         */
        public void cancel() {
            if (D) Log.d(TAG, "ConnectThread cancel");

            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to close null socket");
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        // ==========================================================================
    }
    // ==========================================================================


    /**
     * Класс потока для обмена данными с BT-устройством
     */
    // ==========================================================================
    private class ConnectedThread extends Thread {
        private static final String TAG = "ConnectedThread";
        private static final boolean D = false;

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        // private final OutputStream mmOutStream;


        public ConnectedThread(BluetoothSocket socket) {
            if (D) Log.d(TAG, "create ConnectedThread");

            mmSocket = socket;
            InputStream tmpIn = null;

            //OutputStream tmpOut = null;


            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                //tmpOut = socket.getOutputStream();

            } catch (IOException e) {
                if (D) Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            // mmOutStream = tmpOut;


        }
        // ==========================================================================

        /**
         * Основной рабочий метод - ждёт входящих команд от потока
         */
        public void run() {
            if (D) Log.i(TAG, "ConnectedThread run");


            byte[] buffer = new byte[4096];
            String data = "";
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    int n = mmInStream.read(buffer);
                    Log.d(TAG, "n=" + n);
                    data = String.valueOf(data) + new String(buffer, "ISO-8859-1").substring(0, n);
                    data = (data.replaceAll("\r\n", "\r").replaceAll("\n", "\r"));
                    Log.d(TAG, data);
                    if (!data.contains("\r") && data.length() <= 4096) continue;
                    if (data.contains("\r")) {
                        mHandler.obtainMessage(BluetoothResponseHandler.MESSAGE_DATA, data.lastIndexOf("\r"), -1, data.substring(0, 1 + data.lastIndexOf("\r")).getBytes("ISO-8859-1")).sendToTarget();
                        data = (data.substring(1 + data.lastIndexOf("\r")));
                        continue;
                    }
                    if (data.length() <= 4096) continue;
                    mHandler.obtainMessage(BluetoothResponseHandler.MESSAGE_DATA, data.length(), -1, data.getBytes("ISO-8859-1")).sendToTarget();
                    data = ("");

                }
            } catch (IOException e) {
                e.printStackTrace();
                connectionLost();
            }

        }


// ==========================================================================

        /**
         * Отмена - закрытие сокета
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        // ==========================================================================
    }

// ==========================================================================


}