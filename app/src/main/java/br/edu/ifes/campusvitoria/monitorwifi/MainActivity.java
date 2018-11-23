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
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;

public class MainActivity extends AppCompatActivity {
    public static int INTERVALO_COLETA = 15000;
    public static double LATITUDE = 0.0;
    public static double LONGITUDE = 0.0;
    private DataManager dataManager;
    private String macAddress = "";
    private TelephonyManager telephonyManager;
    static String txtUltimaColeta = "";
    static int nColetasWiFi = 0;
    private Date hora = new Date();
    private MonitorResponseReiver receiver;
    private PendingIntent pendingIntent;
    protected PendingIntent pendingIntentWifi;

    private AlarmManager alarmManager;
    private TextView wifiinfo;
    private TextView txtColetas;
    static int nColetasMobile = 0;
    private String[] permissions = {Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET};
    private boolean statusColeta = true;
    private TextView txtColetasMobile;
    private IntentFilter filter;
    private PowerManager.WakeLock lock;
    private PowerManager pm;
    private TextView txtPeriodoColeta;
    private WifiManager mWifiManager;
    private WifiScanReceiver receiverWifi;
    private IntentFilter filterWifi;

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
        pm = (PowerManager) this.getSystemService( Context.POWER_SERVICE );
        if (pm != null) {
            lock = pm.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, "wl:monitorwifi" );
            lock.acquire();
        }
        txtColetas = findViewById(R.id.txtColetas);
        txtColetasMobile = findViewById(R.id.txtColetasMobile);
        txtPeriodoColeta = findViewById( R.id.txtPeriodoColeta );

        filter = new IntentFilter(MonitorResponseReiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new MonitorResponseReiver();
        Intent intent = new Intent(MainActivity.this, MonitorIntentService.class);
        registerReceiver(receiver, filter);

        filterWifi = new IntentFilter( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION );
        filterWifi.addCategory( Intent.CATEGORY_DEFAULT );
        receiverWifi = new WifiScanReceiver();
        Intent intentWifi = new Intent( MainActivity.this, WifiIntentService.class );
        registerReceiver( receiverWifi, filterWifi );

        pendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
        pendingIntentWifi = PendingIntent.getService( MainActivity.this, 0, intentWifi, 0 );

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
                nColetasWiFi = 0;
                nColetasMobile = 0;
                updateInfoColetas();
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
                updateInfoColetas();
            }
        });
        nColetasMobile = dataManager.countRows("t_mobile");
        nColetasWiFi = dataManager.countRows("t_wifi");
        updateInfoColetas();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        setAlarmManager();
    }

    public void updateInfoColetas() {
        wifiinfo.setText(txtUltimaColeta);
        txtColetas.setText("Número de coletas WiFi: " + nColetasWiFi);
        txtColetasMobile.setText("Número de coletas Mobile: " + nColetasMobile);
        txtPeriodoColeta.setText( "Período de coletas (ms):" + INTERVALO_COLETA );
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterReceiver( receiverWifi );
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
        registerReceiver( receiverWifi, filterWifi );
        updateInfoColetas();
    }

    @Override
    public void onDestroy() {
        // First call the "official" version of this method
        super.onDestroy();
        lock.release();
        //unregisterReceiver(receiver);
        long horaArquivo = hora.getTime();
        String filename_wifi = dataManager.createCSV("t_wifi", macAddress + "_" + horaArquivo);
        String filename_mobile = dataManager.createCSV("t_mobile", macAddress + "_" + horaArquivo);
        Toast.makeText(MainActivity.this, String.format("O arquivo foi salvo em: %s e %s", filename_wifi, filename_mobile), Toast.LENGTH_LONG).show();
        dataManager.deleteAllRecords("t_wifi");
        dataManager.deleteAllRecords("t_mobile");
    }

    public void setAlarmManager() {
//        //long time = System.currentTimeMillis() + INTERVALO_COLETA;
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
        alarmManager.setInexactRepeating( AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVALO_COLETA, pendingIntentWifi );
    }

    public class MonitorResponseReiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "br.edu.ifes.campusvitoria.monitorwifi.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            intent.setClass( context, MonitorResponseReiver.class );
            String text = intent.getStringExtra(MonitorIntentService.PARAM_OUT_MSG);
            updateInfoColetas();
            startWakefulService( context, intent );
        }
    }

    public class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION )) {
                mWifiManager = (WifiManager) getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                List<ScanResult> scanResults = mWifiManager.getScanResults();
                // Write your logic to show in the list
                if (!scanResults.isEmpty()) {
                    for (int i = 0; i < scanResults.size() - 1; i++) {
                        String item = String.valueOf( scanResults.get( i ) );
                        String[] valores = item.split( "," );
                        String[] camposTabela = new String[16];
                        for (int j = 0; j < valores.length; j++) {
                            camposTabela[j] = valores[j].substring( valores[j].indexOf( ":" ) + 2 );
                        }
                        dataManager.insertWiFi( camposTabela[5], camposTabela[0], camposTabela[1], camposTabela[2], camposTabela[3], camposTabela[4], String.valueOf( LATITUDE ), String.valueOf( LONGITUDE ) );
                    }
                    nColetasWiFi++;
                    updateInfoColetas();
                }
            }

        }
    }
}
