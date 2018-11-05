package br.edu.ifes.campusvitoria.monitorwifi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private WifiInfo connectionInfo;
    private TextView textWifiInfo;
    private ImageView imgRSSILevel;
    private String locationProvider;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lastKnownLocation;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;
    //private final PhoneStateListener phoneStateListener = new PhoneStateListener();
    private static final String DEBUG_TAG = "NetworkStatusExample";
    private ConnectivityManager connMgr;
    private final static String LTE_TAG = "LTE_Tag";
    private SignalStrength signalStrength;
    private final static String LTE_SIGNAL_STRENGTH = "getLteSignalStrength";
    private final static String GSM_SIGNAL_STRENGTH = "getGSMSignalStrength";
    private int sensibilidadeSinaldBm;
    private String NetTypeStr;
    private int rssiLevel;
    private List<String> networkSIMs = new ArrayList<>();

    private List<String> getCellSignalStrength(Context context) {
        int strength;
        int j = 0;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
        List<CellInfo> cellInfos = Objects.requireNonNull(telephonyManager).getAllCellInfo();   //This will give info of all sims present inside your mobile
        List<String> operadoras = new ArrayList<>();
        if (cellInfos != null && cellInfos.size() > 0) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i) instanceof CellInfoWcdma) {
                    while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                    }
                    CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) telephonyManager.getAllCellInfo().get(0);
                    CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                    strength = cellSignalStrengthWcdma.getDbm();
                    break;
                } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                    while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                    CellInfoGsm cellInfogsm = (CellInfoGsm) telephonyManager.getAllCellInfo().get(j);
                    j++;
                    if (cellInfogsm.isRegistered()) {
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strength = cellSignalStrengthGsm.getDbm();
                        operadoras.add("GSM;" + cellInfogsm.getCellIdentity().getMnc() + ";" + strength);
                    }

                } else if (cellInfos.get(i) instanceof CellInfoLte) {
                    while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textWifiInfo = findViewById(R.id.wifiinfo);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionInfo = wifiManager.getConnectionInfo();
                refreshInformation();
            }
        });
        imgRSSILevel = findViewById(R.id.imgRSSILevel);
        ImageView imgMAP = findViewById(R.id.imgMAP);

        //acessando servico wifi do android
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectionInfo = Objects.requireNonNull(wifiManager).getConnectionInfo();

        //dados do mobile
        String produto = Build.PRODUCT;
        String modelo = Build.MODEL;
        String fabricante = Build.MANUFACTURER;

        //dados do android
        String codeName = Build.VERSION.CODENAME;
        int versao = Build.VERSION.SDK_INT;
        String realese = Build.VERSION.RELEASE;

        //obtendo dados de localizacao
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

        // Register the listener with the Location Manager to receive location updates
        locationProvider = LocationManager.NETWORK_PROVIDER;
        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        // Remove the listener you previously added
        locationManager.removeUpdates(locationListener);

        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // Listener for the signal strength.
        final PhoneStateListener mListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength sStrength) {
                signalStrength = sStrength;
                rssiLevel = signalStrength.getLevel();
            }
        };

        // Register the listener for the telephony manager
        Objects.requireNonNull(telephonyManager).listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        networkSIMs = getCellSignalStrength(this);
        //atualizando dados na tela
        refreshInformation();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshInformation();
    }

    private void refreshInformation() {
        DataManager dataManager = new DataManager(this);
        NetworkCapabilities networkCapabilities;
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        StringBuilder redeConectada = new StringBuilder();
        String acesso = "";
        String BSSID = connectionInfo.getBSSID();
        String SSID;
        String speed;
        String operadora = "";
        String rede = "";
        String rssi = "";

        // Register the listener with the Location Manager to receive location updates
        locationProvider = LocationManager.NETWORK_PROVIDER;

        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);

        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        // Remove the listener you previously added
        locationManager.removeUpdates(locationListener);
        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();

        networkCapabilities = connMgr.getNetworkCapabilities(connMgr.getActiveNetwork());
        if (networkCapabilities != null) {
            //identificando em qual rede esta conectado: WIFI ou MOBILE e se possui ou nao acesso à internet
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                SSID = networkInfo.getExtraInfo();
                speed = connectionInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS;
                final int NumOfRSSILevels = 4;
                rssiLevel = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), NumOfRSSILevels);
                if (connMgr.getNetworkCapabilities(connMgr.getActiveNetwork()).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    redeConectada = new StringBuilder("Wi-Fi " + networkInfo.getExtraInfo() + " com acesso à Internet\n" + "LinkSpeed: " + connectionInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS);
                } else {
                    redeConectada = new StringBuilder("Wi-Fi " + networkInfo.getExtraInfo() + " sem acesso à Internet\n" + "LinkSpeed: " + connectionInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS);
                }
                dataManager.insertWiFi(SSID, BSSID, speed, String.valueOf(latitude), String.valueOf(longitude));
            } else {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    networkSIMs = getCellSignalStrength(this);
                    for (int i = 0; i < networkSIMs.size(); i++) {
                        String item = networkSIMs.get(i);
                        String[] dados = item.split(";");
                        switch (dados[1]) {
                            case "11":
                                operadora = "Vivo";
                                if (dados[0].equals("GSM")) {
                                    redeConectada.append(" Operadora Vivo com tecnologia ").append(dados[0]).append(" e sensibilidade de sinal (dBm): ").append(dados[2]);
                                } else {
                                    redeConectada.append(" Operadora Vivo com tecnologia ").append(dados[0]).append(" e acesso à Internet e sensibilidade de sinal (dBm): ").append(dados[2]);
                                    rede = dados[0];
                                    rssi = dados[2];
                                }
                                break;
                            case ("2"):
                                operadora = "TIM";
                                if (dados[0].equals("GSM")) {
                                    redeConectada.append(" Operadora TIM com tecnologia ").append(dados[0]).append(" e sensibilidade de sinal (dBm): ").append(dados[2]);
                                } else {
                                    redeConectada.append(" Operadora TIM com tecnologia ").append(dados[0]).append(" e acesso à Internet e sensibilidade de sinal (dBm): ").append(dados[2]);
                                    rede = dados[0];
                                    rssi = dados[2];
                                }
                                break;
                            case ("5"):
                                operadora = "Claro";
                                if (dados[0].equals("GSM")) {
                                    redeConectada.append(" Operadora Claro com tecnologia ").append(dados[0]).append(" e sensibilidade de sinal (dBm): ").append(dados[2]);
                                } else {
                                    redeConectada.append(" Operadora Claro com tecnologia ").append(dados[0]).append(" e acesso à Internet e sensibilidade de sinal (dBm): ").append(dados[2]);
                                    rede = dados[0];
                                    rssi = dados[2];
                                }
                                break;
                            case ("31"):
                                operadora = "Oi";
                                if (dados[0].equals("GSM")) {
                                    redeConectada.append(" Operadora Oi com tecnologia ").append(dados[0]).append(" e sensibilidade de sinal (dBm): ").append(dados[2]);
                                } else {
                                    redeConectada.append(" Operadora Oi com tecnologia ").append(dados[0]).append(" e acesso à Internet e sensibilidade de sinal (dBm): ").append(dados[2]);
                                    rede = dados[0];
                                    rssi = dados[2];
                                }
                                break;
                        }
                    }
                    dataManager.insertMobile(operadora, rede, rssi, String.valueOf(latitude), String.valueOf(longitude));
                }
            }
        }


        //obtendo enderecamento ip da interface wireless
        int ipAddress = connectionInfo.getIpAddress();
        String ipString = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));


        String strWifiInfo = "";
        strWifiInfo += redeConectada + "\n";
        //"Nome do Produto: " + produto + "\n" +
        //"Telefone Modelo: " + modelo + "\n" +
        //"Fabricante: " + fabricante + "\n" +
        //"Codename: " + codeName + "\n" +
        //"API: " + versao + "\n" +
        //"Release: " + realese + "\n" +
        //"Latitude: " + lastKnownLocation.getLatitude() + "\n" +
        //"Longitude: " + lastKnownLocation.getLongitude() + "\n";
        textWifiInfo.setText(strWifiInfo);
        //String url = "https://maps.googleapis.com/maps/api/staticmap?center=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude() + "&zoom=17&format=png&sensor=false&size=640x480&maptype=roadmap&key=AIzaSyDgqVrSKjNkSr-D5DZapKQboCAScAOEJnQ";
        //imgMAP.setImageURI(Uri.parse("https://maps.googleapis.com/maps/api/staticmap?center=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude() + "&zoom=17&format=png&sensor=false&size=640x480&maptype=roadmap&key=AIzaSyDgqVrSKjNkSr-D5DZapKQboCAScAOEJnQ"));
        new DownloadStaticMapTask((ImageView) findViewById(R.id.imgMAP))
                .execute("https://maps.googleapis.com/maps/api/staticmap?&zoom=19&format=png&sensor=false&size=640x480&maptype=roadmap&markers=size:large|color:red|"+ lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude() + "&key=AIzaSyDgqVrSKjNkSr-D5DZapKQboCAScAOEJnQ");

        switch (rssiLevel) {
            case 0:
                imgRSSILevel.setImageResource(R.drawable.wifi_0_bar);
                break;
            case 1:
                imgRSSILevel.setImageResource(R.drawable.wifi_1_bar);
                break;
            case 2:
                imgRSSILevel.setImageResource(R.drawable.wifi_2_bar);
                break;
            case 3:
                imgRSSILevel.setImageResource(R.drawable.wifi_3_bar);
                break;
            case 4:
                imgRSSILevel.setImageResource(R.drawable.wifi_4_bar);
                break;

        }
    }
}
