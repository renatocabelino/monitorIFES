package br.edu.ifes.campusvitoria.monitorwifi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;
    private static final int REQUEST_READ_PHONE_STATE = 1;
    private static String[] PERMISSIONS_READ_PHONE_STATE = {Manifest.permission.READ_PHONE_STATE};
    private DataManager dataManager;
    private String macAddress = "";
    private TelephonyManager telephonyManager;

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
        dataManager = new DataManager(this);
        macAddress = getMacAddr();
        final TextView wifiinfo = (TextView) findViewById(R.id.wifiinfo);
        Button btnExportar = findViewById(R.id.btnExportar);
        btnExportar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename_wifi = dataManager.createCSV("t_wifi", macAddress);
                String filename_mobile = dataManager.createCSV("t_mobile", macAddress);
                Toast.makeText(MainActivity.this, String.format("O arquivo foi salvo em: %s e %s", filename_wifi, filename_mobile), Toast.LENGTH_LONG).show();
                dataManager.deleteAllRecords("t_wifi");
                dataManager.deleteAllRecords("t_mobile");
            }
        });
        Button btnSair = findViewById(R.id.btnSair);
        btnSair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiinfo.setText("Finalizando monitor de conectividade...");
                stopService(new Intent(MainActivity.this, RefreshInformation.class));
                finishAndRemoveTask();
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

        wifiinfo.setText("Inicializando monitor de conectividade ...");
        startService(new Intent(this, RefreshInformation.class)); //start service which is MyService.java
        wifiinfo.setText("Monitor de conectividade inicializado.");
    }
}
