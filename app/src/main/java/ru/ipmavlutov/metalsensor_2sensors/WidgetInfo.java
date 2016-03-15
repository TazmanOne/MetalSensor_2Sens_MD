package ru.ipmavlutov.metalsensor_2sensors;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class WidgetInfo extends AppWidgetProvider {
    public static String ACTION_WIDGET_RECEIVER = "ActionReceiverWidget";
    public static String ACTION_STATE_NONE = "AtionStateNone";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("WidgetInfo", "onReceive");
        //Ловим наш Broadcast, проверяем и выводим сообщение
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_info);
        final String action = intent.getAction();
        if (ACTION_WIDGET_RECEIVER.equals(action)) {
            double msg = intent.getDoubleExtra("signal_value1", 0.0);
            try {
                if ((msg == 0.0)) {
                    views.setImageViewResource(R.id.imageView2, R.drawable.black_monitor);
                }
                if (msg > 0 && msg <= 30) {
                    views.setImageViewResource(R.id.imageView2, R.drawable.black_green);
                }
                if (msg > 30 && msg <= 120) {
                    views.setImageViewResource(R.id.imageView2, R.drawable.black_yellow);
                }
                if (msg > 120) {
                    views.setImageViewResource(R.id.imageView2, R.drawable.black_red);
                }
            } catch (NullPointerException e) {
                Log.e("Error", "msg = null");
            }
        }
        if (ACTION_STATE_NONE.equals(action)) {
            boolean msg = intent.getBooleanExtra("disconnected", false);
            if (msg) {
                views.setImageViewResource(R.id.imageView2, R.drawable.black_monitor);
            }
        }
        ComponentName thiswidget = new ComponentName(context, WidgetInfo.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(thiswidget, views);

    }


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_info);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
            Log.d("WidgetInfo", "onUpdate");
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.d("WidgetInfo", "onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d("WidgetInfo", "onDisabled");
    }
}

