package br.edu.ifes.campusvitoria.monitorwifi;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MonitorIntentService extends IntentService {

    public static final String PARAM_OUT_MSG = "";
    private final static String LTE_TAG = "LTE_Tag";
    private final static String LTE_SIGNAL_STRENGTH = "getLteSignalStrength";
    private final static String GSM_SIGNAL_STRENGTH = "getGSMSignalStrength";
    private WifiManager wifiManager;
    private WifiInfo connectionInfo;
    private String locationProvider;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lastKnownLocation;
    private ConnectivityManager connMgr;
    private SignalStrength signalStrength;
    private int sensibilidadeSinaldBm;
    private String NetTypeStr;
    private int rssiLevel;
    private List<String> networkSIMs = new ArrayList<>();
    private DataManager dataManager;
    private String macAddress = "";
    private TelephonyManager telephonyManager;

    public MonitorIntentService() {
        super("MonitorIntentService");
    }

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
    protected void onHandleIntent(@Nullable Intent intent) {
        dataManager = new DataManager(this);
        Log.i("MonitorServiceIntent: ", "Iniciando tarefa ...");
        String resultTxt = "";
        StringBuilder redeConectada = new StringBuilder();
        String acesso = "";
        String SSID;
        String operadora = "";
        String rede = "";
        String rssi = "";
        java.util.Date timeStamp = new java.util.Date();
        double longitude;
        double latitude;
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                Log.i("wifiMonitor", location.toString());

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        //acessando servico wifi do android
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectionInfo = Objects.requireNonNull(wifiManager).getConnectionInfo();
        String BSSID = connectionInfo.getBSSID();
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //NetworkCapabilities networkCapabilities;
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // Register the listener with the Location Manager to receive location updates
        locationProvider = LocationManager.NETWORK_PROVIDER;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
            lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            // Remove the listener you previously added
            locationManager.removeUpdates(locationListener);
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
        }
        timeStamp.getTime();

        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiConn = networkInfo.isConnected();
        ;
        boolean isMobileConn = networkInfo.isConnected();
        ;
        for (Network network : connMgr.getAllNetworks()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (isWifiConn) {
                    SSID = networkInfo.getExtraInfo();
                    macAddress = getMacAddr();
                    final int NumOfRSSILevels = 4;
                    rssiLevel = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), NumOfRSSILevels);
                    dataManager.insertWiFi(timeStamp.toString(), SSID, BSSID, String.valueOf(rssiLevel), String.valueOf(latitude), String.valueOf(longitude));
                    resultTxt = "Coleta na rede WIFI realizada em " + timeStamp.toString();
                    isWifiConn = false;
                }
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (isMobileConn) {
                    networkSIMs = getCellSignalStrength(this);
                    for (int i = 0; i < networkSIMs.size(); i++) {
                        String item = networkSIMs.get(i);
                        String[] dados = item.split(";");
                        switch (dados[1]) {
                            case "11":
                                operadora = "Vivo";
                                rede = dados[0];
                                rssi = dados[2];

                                break;
                            case ("2"):
                                operadora = "TIM";
                                rede = dados[0];
                                rssi = dados[2];
                                break;
                            case ("5"):
                                operadora = "Claro";

                                rede = dados[0];
                                rssi = dados[2];
                                break;
                            case ("31"):
                                operadora = "Oi";
                                rede = dados[0];
                                rssi = dados[2];
                                break;
                        }
                    }
                    dataManager.insertMobile(timeStamp.toString(), operadora, rede, rssi, String.valueOf(latitude), String.valueOf(longitude));
                    resultTxt = "Coleta na rede Móvel realizada em " + timeStamp.toString();
                    isMobileConn = false;
                }

            }
        }

//        networkCapabilities = connMgr.getNetworkCapabilities(connMgr.getActiveNetwork());
//        if (networkCapabilities != null) {
//            //identificando em qual rede esta conectado: WIFI ou MOBILE e se possui ou nao acesso à internet
//            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//                SSID = networkInfo.getExtraInfo();
//                macAddress = getMacAddr();
//                speed = connectionInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS;
//                final int NumOfRSSILevels = 4;
//                rssiLevel = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), NumOfRSSILevels);
//                dataManager.insertWiFi(timeStamp.toString(), SSID, BSSID, String.valueOf(rssiLevel), String.valueOf(latitude), String.valueOf(longitude));
//                resultTxt = "Coleta na rede WIFI realizada em " + timeStamp.toString();
//            } else {
//                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
//                    networkSIMs = getCellSignalStrength(this);
//                    for (int i = 0; i < networkSIMs.size(); i++) {
//                        String item = networkSIMs.get(i);
//                        String[] dados = item.split(";");
//                        switch (dados[1]) {
//                            case "11":
//                                operadora = "Vivo";
//                                rede = dados[0];
//                                rssi = dados[2];
//
//                                break;
//                            case ("2"):
//                                operadora = "TIM";
//                                rede = dados[0];
//                                rssi = dados[2];
//                                break;
//                            case ("5"):
//                                operadora = "Claro";
//
//                                rede = dados[0];
//                                rssi = dados[2];
//                                break;
//                            case ("31"):
//                                operadora = "Oi";
//                                rede = dados[0];
//                                rssi = dados[2];
//                                break;
//                        }
//                    }
//                    dataManager.insertMobile(timeStamp.toString(), operadora, rede, rssi, String.valueOf(latitude), String.valueOf(longitude));
//                    resultTxt = "Coleta na rede WIFI realizada em " + timeStamp.toString();
//                } else {
//                    resultTxt = networkCapabilities.toString();
//                }
//            }
//        } else {
//            resultTxt = "NetworkCapability null";
//        }
        Log.i("MonitorIntentService: ", "Finalizando tarefa ...");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.MonitorResponseReiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
        sendBroadcast(broadcastIntent);
    }

    private List<String> getCellSignalStrength(Context context) {
        int strength;
        int j = 0;
        List<String> operadoras = new ArrayList<>();
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return operadoras;
        } else {
            List<CellInfo> cellInfos = Objects.requireNonNull(telephonyManager).getAllCellInfo();   //This will give info of all sims present inside your mobile
            if (cellInfos != null && cellInfos.size() > 0) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i) instanceof CellInfoWcdma) {

                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) telephonyManager.getAllCellInfo().get(0);
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                        strength = cellSignalStrengthWcdma.getDbm();
                        break;
                    } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                        CellInfoGsm cellInfogsm = (CellInfoGsm) telephonyManager.getAllCellInfo().get(j);
                        j++;
                    } else if (cellInfos.get(i) instanceof CellInfoLte) {

                        CellInfoLte cellInfoLte = (CellInfoLte) telephonyManager.getAllCellInfo().get(i);
                        if (cellInfoLte.isRegistered()) {
                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                            strength = cellSignalStrengthLte.getDbm();
                            operadoras.add("LTE;" + cellInfoLte.getCellIdentity().getMnc() + ";" + strength);
                        }

                    }
                }

            }
            return operadoras;
        }

    }
}
