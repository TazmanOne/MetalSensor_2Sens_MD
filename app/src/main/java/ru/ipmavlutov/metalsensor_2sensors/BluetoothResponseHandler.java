package ru.ipmavlutov.metalsensor_2sensors;


import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

import ru.ipmavlutov.metalsensor_2sensors.Activities.Work;

public class BluetoothResponseHandler extends Handler {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final int MESSAGE_DATA = 4;
    private String MSG_CONNECTING;
    private WeakReference<Work> mActivity;
    private String MSG_CONNECTED;
    private String MSG_NOT_CONNECTED;
    private double Z;
    private boolean first_start;

    public BluetoothResponseHandler(Work activity) {

        mActivity = new WeakReference<>(activity);
        MSG_CONNECTED = activity.MSG_CONNECTED;
        MSG_NOT_CONNECTED = activity.MSG_NOT_CONNECTED;
        MSG_CONNECTING = activity.MSG_CONNECTING;
        Z = Work.Z;
        first_start = true;

    }

    public void setTarget(Work target) {
        mActivity.clear();
        mActivity = new WeakReference<>(target);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        Work activity = mActivity.get();
        if (activity != null) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    final ActionBar bar = activity.getSupportActionBar();
                    switch (msg.arg1) {
                        case DeviceConnector.STATE_CONNECTED:
                            assert bar != null;
                            bar.setSubtitle(MSG_CONNECTED);
                        case DeviceConnector.STATE_CONNECTING:
                            assert bar != null;
                            bar.setSubtitle(MSG_CONNECTING);
                            break;
                        case DeviceConnector.STATE_NONE:
                            assert bar != null;
                            bar.setSubtitle(MSG_NOT_CONNECTED);
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    activity.setDeviceName((String) msg.obj);
                    break;
                case MESSAGE_TOAST:
                    Intent action_disconnected = new Intent(activity,WidgetInfo.class);
                    action_disconnected.setAction(WidgetInfo.ACTION_STATE_NONE);
                    action_disconnected.putExtra("disconnected",true);
                    activity.sendBroadcast(action_disconnected);

                    Toast.makeText(activity.getBaseContext(), "Устройство " + msg.obj + " отключено", Toast.LENGTH_LONG).show();
                    activity.myTimer_off();
                    activity.finish();
                    /*Intent start = new Intent(activity.getBaseContext(), Welcome.class);
                    activity.startActivityForResult(start, 0);*/
                    break;
                case MESSAGE_DATA:
                    byte[] a = (byte[]) msg.obj;
                    //activity.UpDateResult(a);
                    if (a.length == 19) {
                        byte[] temperature_1 = Arrays.copyOfRange(a, 0, 2);//[0,1]
                        byte[] signal_1 = Arrays.copyOfRange(a, 2, 7);//[2,6]
                        byte[] temperature_2 = Arrays.copyOfRange(a, 7, 9);//[7,8]
                        byte[] signal_2 = Arrays.copyOfRange(a, 9, 14);//[9,13]

                        //byte[] super_signal = Arrays.copyOfRange(a, 4, 6);//[4,5]
                        //FF=[6,7],/r/n=[8,9]

                        //температура датчика #1
                        int temperature_result_1 = FindTemperature(GetTemperature(temperature_1));
                        TextView temperature_value_1 = (TextView) activity.findViewById(R.id.temperature_value_1);
                        temperature_value_1.setText(String.format("%s  °C", String.valueOf(temperature_result_1)));

                        //значение сигнала #1


                        TextView signal_value_1 = (TextView) activity.findViewById(R.id.signal_value_1);
                        String str_signal_1 = new String(signal_1, Charset.forName("UTF-8"));
                        signal_value_1.setText(String.format("%s мг", str_signal_1));


                        double signal_result_1=Double.parseDouble(str_signal_1.replace(",",".") );

                        //температура датчика #2
                        int temperature_result_2 = FindTemperature(GetTemperature(temperature_2));
                        TextView temperature_value_2 = (TextView) activity.findViewById(R.id.temperature_value_2);
                        temperature_value_2.setText(String.format("%s  °C", String.valueOf(temperature_result_2)));

                        //значение сигнала #2
                        //double signal_result_2 = FindRudeSignal(Correction(GetSignal(signal_2), temperature_result_2), Z);
                        TextView signal_value_2 = (TextView) activity.findViewById(R.id.signal_value_2);
                        String str_signal_2 = new String(signal_2, Charset.forName("UTF-8"));
                        signal_value_2.setText(String.format("%s мг", str_signal_2));


                        //значение супер сигнала
                        /*double super_signal_result = FindSuperSignal(Correction(GetSuperSignal(super_signal), temperature_result_1), Z);
                        TextView super_signal_value = (TextView) activity.findViewById(R.id.super_signal_value);
                        super_signal_value.setText(String.format("%s мг", Double.toString(super_signal_result)));*/

                        //на время super_signal_result = 0
                        double super_signal_result = 0;
                        activity.UpDateBD(temperature_result_1, signal_result_1, super_signal_result);

                        activity.myTimer(first_start);
                        first_start = false;

                        Intent action = new Intent(activity,WidgetInfo.class);
                        action.setAction(WidgetInfo.ACTION_WIDGET_RECEIVER);
                        action.putExtra("signal_value1",signal_result_1);
                        //PendingIntent actionPendingIntent = PendingIntent.getBroadcast(activity, 0, action, 0);
                        activity.sendBroadcast(action);


                    }

            }
        }
    }

    private short GetTemperature(byte[] temperature_array) {
        short temperature;
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(temperature_array[0]);
        bb.put(temperature_array[1]);
        temperature = bb.getShort(0);
        return temperature;
    }

    private double GetSuperSignal(byte[] super_signal_array) {
        double super_signal;
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(super_signal_array[0]);
        bb.put(super_signal_array[1]);
        super_signal = bb.getShort(0);
        return super_signal;
    }

    private double GetSignal(byte[] signal_array) {
        double signal;
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(signal_array[0]);
        bb.put(signal_array[1]);
        signal = bb.getShort(0);
        return signal;
    }

    private int temperature_code[] =
            {
                    0x01F7, 0x01F6, 0x01F5, 0x01F4, 0x01F3, 0x01F2, 0x01F1, 0x01F0, 0x01EF, 0x01EE,
                    0x01ED, 0x01EC, 0x01EA, 0x01E9, 0x01E8, 0x01E6, 0x01E5, 0x01E3, 0x01E2, 0x01E0,
                    0x01DE, 0x01DC, 0x01DB, 0x01D9, 0x01D6, 0x01D4, 0x01D2, 0x01D0, 0x01CD, 0x01CA,
                    0x01C7, 0x01C5, 0x01C2, 0x01BF, 0x01BC, 0x00B9, 0x01B6, 0x01B3, 0x01AF, 0x01AB,
                    0x01A7, 0x01A4, 0x01A0, 0x019C, 0x0198, 0x0193, 0x0190, 0x018B, 0x0187, 0x0182,
                    0x017D, 0x0179, 0x0174, 0x0170, 0x016A, 0x0165, 0x0160, 0x015B, 0x0156, 0x0151,
                    0x014B, 0x0146, 0x0141, 0x013C, 0x0136, 0x0130, 0x012B, 0x0126, 0x0121, 0x011B,
                    0x0115, 0x0110, 0x010B, 0x0106, 0x0100, 0x00FA, 0x00F6, 0x00F1, 0x00EB, 0x00E6,
                    0x00E0, 0x00DC, 0x00D7, 0x00D2, 0x00CD, 0x00C8, 0x00C3, 0x00BF, 0x00BA, 0x00B6,
                    0x00B1, 0x00AD, 0x00A9, 0x00A4, 0x00A0, 0x009C, 0x0098, 0x0094, 0x0091, 0x008D,
                    0x0089, 0x0085, 0x0082, 0x007F, 0x007B, 0x0078, 0x0075, 0x0072, 0x006F, 0x006C,
                    0x0068, 0x0066, 0x0063, 0x0060, 0x005E, 0x005B, 0x0059, 0x0057, 0x0054, 0x0052,
                    0x004F, 0x004D, 0x004B, 0x0049, 0x0047, 0x0045, 0x0043, 0x0042, 0x0040, 0x003E,
                    0x003C, 0x003B, 0x0039, 0x0038, 0x0036, 0x0035, 0x0033, 0x0032, 0x0031, 0x002F,
                    0x002E
            };


    public int FindTemperature(int thermo) {
        int index;
        int temperature;


        if (thermo >= 0x01F8) {
            return 127;
        } else if (thermo >= 0x01A8) {
            index = 40;
        } else if (thermo >= 0x0116) {
            index = 70;
        } else if (thermo >= 0x008A) {
            index = 100;
        } else if (thermo >= 0x002E) {
            index = 140;
        } else {
            return (thermo + 54);
        }
        while ((temperature_code[index--]) < thermo) ;
        temperature = ((index) - 39);
        return temperature;
    }

    public double FindRudeSignal(double P, double Z) {
        double m;
        double dp;
        double result;
        dp = P - Z;//104
        if (dp < 3) {
            m = 0;
        } else {
            if (dp < 9) {//8
                m = 0.075 * dp;

            } else {
                if (dp < 27) {
                    m = 0.0473 * dp + 0.2229;
                } else {
                    if (dp < 51) {
                        m = 0.10416 * dp - 1.3121;
                    } else {
                        m = 0.375 * dp - 15.125;
                    }
                }
            }
        }

        result = new BigDecimal(m).setScale(1, RoundingMode.UP).doubleValue();
        return (result);
    }

    // final int SP0 = 533;
    public double FindSuperSignal(double P, double Z) {
        double m;
        double dp;
        double super_result;
        dp = P - Z;
        if (dp < 85) {
            m = 0;
        } else {
            if (dp < 270) {
                m = 0.00486 * dp + 0.187;
            } else {
                if (dp < 510) {
                    m = 0.010476 * dp - 1.3123;
                } else {
                    m = 0.0375 * dp - 15.125;
                }
            }
        }

        super_result = new BigDecimal(m).setScale(1, RoundingMode.UP).doubleValue();
        return (super_result);
    }

    public double Correction(double P, int temperature) {
        double correct_signal;
        double correction;
        if (temperature < 76) {
            correction = 0.45 * temperature - 11.25;
        } else {
            correction = 0.29 * temperature + 0.96;
        }
        correct_signal = P - correction;
        return correct_signal;
    }
}


