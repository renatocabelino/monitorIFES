package br.edu.ifes.campusvitoria.monitorwifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;
    private static final int REQUEST_READ_PHONE_STATE = 1;
    private static String[] PERMISSIONS_READ_PHONE_STATE = {Manifest.permission.READ_PHONE_STATE};
    private DataManager dataManager;
    private String macAddress = "";
    private TelephonyManager telephonyManager;
    public static int INTERVALO_COLETA = 30000;
    private boolean statusColeta = false;
    private Date hora = new Date();
    private MonitorResponseReiver receiver;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(MonitorResponseReiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new MonitorResponseReiver();
        Intent intent = registerReceiver(receiver, filter);
        dataManager = new DataManager(this);
        macAddress = getMacAddr();
        final TextView wifiinfo = (TextView) findViewById(R.id.wifiinfo);
        Button btnExportar = findViewById(R.id.btnExportar);
        btnExportar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long horaArquivo = hora.getTime();
                String filename_wifi = dataManager.createCSV("t_wifi", macAddress + "_" + horaArquivo);
                String filename_mobile = dataManager.createCSV("t_mobile", macAddress + "_" + horaArquivo);
                Toast.makeText(MainActivity.this, String.format("O arquivo foi salvo em: %s e %s", filename_wifi, filename_mobile), Toast.LENGTH_LONG).show();
                dataManager.deleteAllRecords("t_wifi");
                dataManager.deleteAllRecords("t_mobile");
                //txtnColetas.setText(String.valueOf(N_COLETAS_REALIZADAS));
            }
        });
        final Button btnSair = findViewById(R.id.btnSair);
        btnSair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    wifiinfo.setText("Monitor de conectividade iniciado.");
                Intent monitorIntent = new Intent(MainActivity.this, MonitorIntentService.class);
                startService(monitorIntent);
            }
        });
        final TextView txtInputFreq = findViewById(R.id.inputFreq);
        ImageButton btnConfirmaFreq = findViewById(R.id.btnConfirmaFreq);

        btnConfirmaFreq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                stopService(new Intent(MainActivity.this, RefreshInformation.class));
//                INTERVALO_COLETA = Integer.parseInt(("" + txtInputFreq.getText()));
//                //txtIntervaloColetas.setText(String.valueOf(INTERVALO_COLETA));
//                startService(new Intent(MainActivity.this, RefreshInformation.class));
            }
        });

        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_READ_PHONE_STATE, REQUEST_READ_PHONE_STATE);
        }

        /*wifiinfo.setText("Inicializando monitor de conectividade ...");
        startService(new Intent(this, RefreshInformation.class)); //start service which is MyService.java
        wifiinfo.setText("Monitor de conectividade inicializado.");
        */
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

    public class MonitorResponseReiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "br.edu.ifes.campusvitoria.monitorwifi.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            TextView result = (TextView) findViewById(R.id.wifiinfo);
            String text = intent.getStringExtra(MonitorIntentService.PARAM_OUT_MSG);
            result.setText(text);
        }
    }
}
