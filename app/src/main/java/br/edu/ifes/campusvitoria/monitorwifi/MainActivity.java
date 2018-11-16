package br.edu.ifes.campusvitoria.monitorwifi;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static int INTERVALO_COLETA = 6000;
    private DataManager dataManager;
    private String macAddress = "";
    private TelephonyManager telephonyManager;
    private String[] permissions = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private boolean statusColeta = false;
    private Date hora = new Date();
    private MonitorResponseReiver receiver;
    private PendingIntent pendingIntent;

    private AlarmManager alarmManager;
    private GregorianCalendar calendar;
    private TextView wifiinfo;
    private TextView txtColetas;
    private int nColetas = 0;

    private static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    String hex = Integer.toHexString(b & 0xFF);
                    if (hex.length() == 1)
                        hex = "0".concat(hex);
                    res1.append(hex.concat(":"));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "";
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message message) {
            if (message.arg1 == RESULT_OK) {
                wifiinfo.setText(message.arg2);
                //setAlarmManager();
            }
        }

        ;
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestMultiplePermissions() {
        List<String> remainingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(permission);
            }
        }
        if (!remainingPermissions.isEmpty()) {
            requestPermissions(remainingPermissions.toArray(new String[remainingPermissions.size()]), 101);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(permissions[i])) {
                        new AlertDialog.Builder(this)
                                .setMessage("Your error message here")
                                .setPositiveButton("Allow", (dialog, which) -> requestMultiplePermissions())
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .create()
                                .show();
                    }
                    return;
                }
            }
            //all is good, continue flow
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestMultiplePermissions();
        txtColetas = findViewById(R.id.txtColetas);

        IntentFilter filter = new IntentFilter(MonitorResponseReiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new MonitorResponseReiver();
        Intent intent = registerReceiver(receiver, filter);
        intent = new Intent(MainActivity.this, MonitorIntentService.class);
        pendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
        dataManager = new DataManager(this);
        macAddress = getMacAddr();
        wifiinfo = (TextView) findViewById(R.id.wifiinfo);
        Button btnExportar = findViewById(R.id.btnExportar);
        btnExportar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long horaArquivo = hora.getTime();
                dataManager.openDB();
                String filename_wifi = dataManager.createCSV("t_wifi", macAddress + "_" + horaArquivo);
                String filename_mobile = dataManager.createCSV("t_mobile", macAddress + "_" + horaArquivo);
                Toast.makeText(MainActivity.this, String.format("O arquivo foi salvo em: %s e %s", filename_wifi, filename_mobile), Toast.LENGTH_LONG).show();
                dataManager.deleteAllRecords("t_wifi");
                dataManager.deleteAllRecords("t_mobile");
                dataManager.closeDB();
                nColetas = 0;
                txtColetas.setText("Número de coletas: " + nColetas);
            }
        });
        final Button btnSair = findViewById(R.id.btnSair);

        btnSair.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (statusColeta) {
                    statusColeta = false;
                    btnSair.setText("Iniciar Coleta");
                    alarmManager.cancel(pendingIntent);
                    dataManager.closeDB();
                } else {
                    dataManager.openDB();
                    statusColeta = true;
                    btnSair.setText("Parar Coleta");
                    setAlarmManager();
                }
            }
        });
        final TextView txtInputFreq = findViewById(R.id.inputFreq);
        ImageButton btnConfirmaFreq = findViewById(R.id.btnConfirmaFreq);

        btnConfirmaFreq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alarmManager.cancel(pendingIntent);
                INTERVALO_COLETA = Integer.parseInt(("" + txtInputFreq.getText()));
                setAlarmManager();
            }
        });
        /*wifiinfo.setText("Inicializando monitor de conectividade ...");
        startService(new Intent(this, RefreshInformation.class)); //start service which is MyService.java
        wifiinfo.setText("Monitor de conectividade inicializado.");
        */
        calendar = (GregorianCalendar) Calendar.getInstance();
        Messenger messenger = new Messenger(handler);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        setAlarmManager();
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        // First call the "official" version of this method
        super.onDestroy();
        unregisterReceiver(receiver);
        long horaArquivo = hora.getTime();
        String filename_wifi = dataManager.createCSV("t_wifi", macAddress + "_" + horaArquivo);
        String filename_mobile = dataManager.createCSV("t_mobile", macAddress + "_" + horaArquivo);
        Toast.makeText(MainActivity.this, String.format("O arquivo foi salvo em: %s e %s", filename_wifi, filename_mobile), Toast.LENGTH_LONG).show();
        dataManager.deleteAllRecords("t_wifi");
        dataManager.deleteAllRecords("t_mobile");
    }

    public void setAlarmManager() {

//        if (Build.VERSION.SDK_INT >= 23) {
//            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, INTERVALO_COLETA, pendingIntent);
//        } else {
//            if (Build.VERSION.SDK_INT >= 19) {
//                alarmManager.setExact(AlarmManager.RTC_WAKEUP, INTERVALO_COLETA, pendingIntent);
//            } else {
//                alarmManager.set(AlarmManager.RTC_WAKEUP, INTERVALO_COLETA, pendingIntent);
//            }
//        }
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVALO_COLETA, pendingIntent);
    }

    public class MonitorResponseReiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "br.edu.ifes.campusvitoria.monitorwifi.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            TextView result = (TextView) findViewById(R.id.wifiinfo);
            String text = intent.getStringExtra(MonitorIntentService.PARAM_OUT_MSG);
            result.setText(text);
            nColetas++;
            TextView txtColetas = findViewById(R.id.txtColetas);
            txtColetas.setText("Número de coletas: " + nColetas);
            final PendingIntent pendingIntent;
            pendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
            //setAlarmManager();
        }
    }
}
