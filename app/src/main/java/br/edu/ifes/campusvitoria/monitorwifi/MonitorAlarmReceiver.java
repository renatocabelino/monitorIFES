package br.edu.ifes.campusvitoria.monitorwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MonitorAlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;
    public static final String ACTION = "br.edu.ifes.campusvitoria.monitorwifi.MonitorAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MonitorIntentService.class);
        context.startService(i);
    }
}
