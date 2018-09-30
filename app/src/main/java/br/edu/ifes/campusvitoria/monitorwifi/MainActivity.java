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
import android.support.v4.content.ContextCompat;
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

public class MainActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private WifiInfo connectionInfo;
    private String ipString = new String();
    private TextView textWifiInfo;
    private ImageView imgRSSILevel;
    private ImageView imgMAP;
    private String produto;
    private String modelo;
    private String fabricante;
    private String codeName;
    private int versao;
    private String realese;
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
    private NetworkInfo networkInfo;
    private SignalStrength      signalStrength;
    private TelephonyManager    telephonyManager;
    private final static String LTE_TAG             = "LTE_Tag";
    private final static String LTE_SIGNAL_STRENGTH = "getLteSignalStrength";
    private final static String GSM_SIGNAL_STRENGTH = "getGSMSignalStrength";
    private int sensibilidadeSinaldBm;
    private String NetTypeStr;
    private int rssiLevel;
    private String BSSID;
    private double latitude;
    private double longitude;
    private List<String> networkSIMs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textWifiInfo = (TextView) findViewById(R.id.wifiinfo);
        Button btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionInfo = wifiManager.getConnectionInfo();
                refreshInformation();
            }
        });
        imgRSSILevel = (ImageView) findViewById(R.id.imgRSSILevel);
        imgMAP = (ImageView) findViewById(R.id.imgMAP);

        //acessando servico wifi do android
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        connectionInfo = wifiManager.getConnectionInfo();

        //dados do mobile
        produto = Build.PRODUCT;
        modelo = Build.MODEL;
        fabricante = Build.MANUFACTURER;

        //dados do android
        codeName = Build.VERSION.CODENAME;
        versao = Build.VERSION.SDK_INT;
        realese = Build.VERSION.RELEASE;

        //obtendo dados de localizacao
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                Log.i("wifiMonitor", location.toString());

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
        // Register the listener with the Location Manager to receive location updates
        locationProvider = LocationManager.NETWORK_PROVIDER;
        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        // Remove the listener you previously added
        locationManager.removeUpdates(locationListener);

        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // Listener for the signal strength.
        final PhoneStateListener mListener = new PhoneStateListener()
        {
            @Override
            public void onSignalStrengthsChanged(SignalStrength sStrength)
            {
                signalStrength = sStrength;
                rssiLevel = signalStrength.getLevel();
            }
        };

        // Register the listener for the telephony manager
        telephonyManager.listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        networkSIMs = getCellSignalStrength(this);
        //atualizando dados na tela
        refreshInformation();

    }
    @Override
    public void onResume(){
        super.onResume();
        refreshInformation();
    }

    public static List<String> getCellSignalStrength(Context context) {
        int strength = 0;
        int j=0;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
        List<String> operadoras = new ArrayList<>();
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
                    if (cellInfogsm.isRegistered()) {
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strength = cellSignalStrengthGsm.getDbm();
                        operadoras.add("GSM;" + cellInfogsm.getCellIdentity().getMnc() + ";" + strength );
                    }

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


    public void refreshInformation () {
        NetworkCapabilities networkCapabilities;
        networkInfo = connMgr.getActiveNetworkInfo();
        String redeConectada = "";
        String acesso = "";
        BSSID = connectionInfo.getBSSID();
        networkCapabilities = connMgr.getNetworkCapabilities(connMgr.getActiveNetwork());
        if (networkCapabilities != null) {
            //identificando em qual rede esta conectado: WIFI ou MOBILE e se possui ou nao acesso à internet
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                final int NumOfRSSILevels = 4;
                rssiLevel = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), NumOfRSSILevels);
                if (connMgr.getNetworkCapabilities(connMgr.getActiveNetwork()).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    redeConectada = "Wi-Fi " + networkInfo.getExtraInfo() + " com acesso à Internet\n" + "LinkSpeed: " + connectionInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS;
                } else {
                    redeConectada = "Wi-Fi " + networkInfo.getExtraInfo() + " sem acesso à Internet\n" + "LinkSpeed: " + connectionInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS;
                }
            } else {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    networkSIMs = getCellSignalStrength(this);
                    for (int i = 0; i < networkSIMs.size(); i++) {
                        String item = networkSIMs.get(i);
                        String[] dados = item.split(";");
                        switch (dados[1]) {
                            case "11":
                                if (dados[0].equals("GSM")) {
                                    redeConectada = redeConectada + " Operadora Vivo com tecnologia " + dados[0] + " e sensibilidade de sinal (dBm): " + dados[2];
                                } else {
                                    redeConectada = redeConectada + " Operadora Vivo com tecnologia " + dados[0] + " e acesso à Internet e sensibilidade de sinal (dBm): " + dados[2];
                                }
                                break;
                            case ("2"):
                                if (dados[0].equals("GSM")) {
                                    redeConectada = redeConectada + " Operadora TIM com tecnologia " + dados[0] + " e sensibilidade de sinal (dBm): " + dados[2];
                                } else {
                                    redeConectada = redeConectada + " Operadora TIM com tecnologia " + dados[0] + " e acesso à Internet e sensibilidade de sinal (dBm): " + dados[2];
                                }
                                break;
                            case ("5"):
                                if (dados[0].equals("GSM")) {
                                    redeConectada = redeConectada + " Operadora Claro com tecnologia " + dados[0] + " e sensibilidade de sinal (dBm): " + dados[2];
                                } else {
                                    redeConectada = redeConectada + " Operadora Claro com tecnologia " + dados[0] + " e acesso à Internet e sensibilidade de sinal (dBm): " + dados[2];
                                }
                                break;
                            case ("31"):
                                if (dados[0].equals("GSM")) {
                                    redeConectada = redeConectada + " Operadora Oi com tecnologia " + dados[0] + " e sensibilidade de sinal (dBm): " + dados[2];
                                } else {
                                    redeConectada = redeConectada + " Operadora Oi com tecnologia " + dados[0] + " e acesso à Internet e sensibilidade de sinal (dBm): " + dados[2];
                                }
                                break;
                        }
                    }
                }
            }
        }


        // Register the listener with the Location Manager to receive location updates
        locationProvider = LocationManager.NETWORK_PROVIDER;
        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        // Remove the listener you previously added
        locationManager.removeUpdates(locationListener);

        //obtendo enderecamento ip da interface wireless
        int ipAddress = connectionInfo.getIpAddress();
        ipString = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));

        latitude = lastKnownLocation.getLatitude();
        longitude = lastKnownLocation.getLongitude();
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
