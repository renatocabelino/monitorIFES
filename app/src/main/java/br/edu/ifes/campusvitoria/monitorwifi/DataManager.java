package br.edu.ifes.campusvitoria.monitorwifi;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;

public class DataManager {
    /*
        Next we have a public static final string for
        each row/table that we need to refer to both
        inside and outside this class
    */
    public static final String TABLE_ROW_ID = "_id";
    public static final String TABLE_ROW_TIMESTAMP = "timestamp";
    public static final String TABLE_ROW_BSSID = "bssid";
    public static final String TABLE_ROW_SSID = "ssid";
    public static final String TABLE_ROW_SPEED = "speed";
    public static final String TABLE_ROW_OPERADORA = "operadora";
    public static final String TABLE_ROW_REDE = "rede";
    public static final String TABLE_ROW_RSSI = "rssi";
    public static final String TABLE_ROW_LATITUDE = "latitude";
    public static final String TABLE_ROW_LONGITUDE = "longitude";
    /*
    Next we have a private static final strings for
    each row/table that we need to refer to just
    inside this class
    */
    private static final String DB_NAME = "monitor_wifi_db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_WIFI = "t_wifi";
    private static final String TABLE_MOBILE = "t_mobile";

    private String filename = "";

    // This is the actual database
    private SQLiteDatabase db;

    public DataManager(Context context) {
        // Create an instance of our internal
        //CustomSQLiteOpenHelper class
        CustomSQLiteOpenHelper helper = new CustomSQLiteOpenHelper(context);
        // Get a writable database
        db = helper.getWritableDatabase();
    }

    // Insert a record
    public void insertWiFi(String timeStamp, String ssid, String bssid, String speed, String latitude, String longitude) {
        // Add all the details to the table
        String query = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES ('%s', '%s', '%s', '%s', '%s', '%s');", TABLE_WIFI, TABLE_ROW_TIMESTAMP, TABLE_ROW_SSID, TABLE_ROW_BSSID, TABLE_ROW_SPEED, TABLE_ROW_LATITUDE, TABLE_ROW_LONGITUDE, timeStamp, ssid, bssid, speed, latitude, longitude);
        Log.i("insert() = ", query);
        db.execSQL(query);
    }

    public void insertMobile(String timeStamp, String operadora, String rede, String rssi, String latitude, String longitude) {
        // Add all the details to the table
        String query = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES ('%s', '%s', '%s', '%s', '%s', '%s');", TABLE_MOBILE, TABLE_ROW_TIMESTAMP, TABLE_ROW_OPERADORA, TABLE_ROW_REDE, TABLE_ROW_RSSI, TABLE_ROW_LATITUDE, TABLE_ROW_LONGITUDE, timeStamp, operadora, rede, rssi, latitude, longitude);
        Log.i("insert() = ", query);
        db.execSQL(query);
    }

    // Get all the records
    public Cursor selectAll(String table) {
        Cursor c = db.rawQuery("SELECT *" + " from " + table, null);
        return c;
    }

    //delete all records
    public void deleteAllRecords(String tableName) {
        // Add all the details to the table
        String query = String.format("DELETE from %s;", tableName);
        Log.i("delete() = ", query);
        db.execSQL(query);
    }

    // metodo para criar o csv
    public String createCSV(String table_name, String macAddress) {
        macAddress = macAddress.replaceAll(":", "_");
        boolean var = true;
        String state = Environment.getExternalStorageState();
        //external storage availability check
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e("monitorWiFi", "Media not mounted.");
        }
        File folder = new File(String.valueOf(Environment.getExternalStorageDirectory().getPath()), Environment.DIRECTORY_DOCUMENTS);
        if (!folder.exists())
            var = folder.mkdir();
        if (table_name.equals(TABLE_WIFI)) {
            filename = folder.toString() + "/" + macAddress + "wifi.csv";
        } else {
            if (table_name.equals(TABLE_MOBILE)) {
                filename = folder.toString() + "/" + macAddress + "mobile.csv";
            }
        }


        Uri u1 = null;

        Cursor cursor = selectAll(table_name);


        try {
            FileWriter fw = new FileWriter(filename);
            if (cursor != null) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    for (int j = 0; j < cursor.getColumnNames().length; j++) {
                        fw.append(cursor.getString(j) + ";");
                    }
                    fw.append("\n");

                    cursor.moveToNext();
                }
                cursor.close();
            }
            fw.close();
        } catch (Exception e) {
            Log.e("monitorWifFi", String.valueOf(e));
        }
        return filename;
    }

    //delete table
    public void deleteTable(String tableName) {
        String newTableQueryString = String.format("drop table %s;", tableName);
        Log.i("monitorWifi:  ", newTableQueryString);
        db.execSQL(newTableQueryString);
    }

    //create table
    public void createTableWifi(String tableName) {
        // Create a table for wifi data
        String newTableQueryString = String.format("create table if not exists %s (%s integer primary key autoincrement not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null);", tableName, TABLE_ROW_ID, TABLE_ROW_TIMESTAMP, TABLE_ROW_SSID, TABLE_ROW_BSSID, TABLE_ROW_SPEED, TABLE_ROW_LATITUDE, TABLE_ROW_LONGITUDE);
        Log.i("create(): ", newTableQueryString);
        db.execSQL(newTableQueryString);
    }

    //create table
    public void createTableMobile(String tableName) {
        // Create a table for wifi data
        String newTableQueryString = String.format("create table if not exists %s (%s integer primary key autoincrement not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null);", tableName, TABLE_ROW_ID, TABLE_ROW_TIMESTAMP, TABLE_ROW_OPERADORA, TABLE_ROW_REDE, TABLE_ROW_RSSI, TABLE_ROW_LATITUDE, TABLE_ROW_LONGITUDE);
        Log.i("create(): ", newTableQueryString);
        db.execSQL(newTableQueryString);
    }

    // This class is created when our DataManager is initialized
    private class CustomSQLiteOpenHelper extends SQLiteOpenHelper {
        public CustomSQLiteOpenHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }
        // This method only runs the first time the database is created
        @Override
        public void onCreate(SQLiteDatabase db) {

            // Create a table for wifi data
            String newTableQueryString = String.format("create table if not exists %s (%s integer primary key autoincrement not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null);", TABLE_WIFI, TABLE_ROW_ID, TABLE_ROW_TIMESTAMP, TABLE_ROW_SSID, TABLE_ROW_BSSID, TABLE_ROW_SPEED, TABLE_ROW_LATITUDE, TABLE_ROW_LONGITUDE);
            Log.i("create(): ", newTableQueryString);
            db.execSQL(newTableQueryString);
            // Create a table for mobile data
            newTableQueryString = String.format("create table if not exists %s (%s integer primary key autoincrement not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null,%s text not null);", TABLE_MOBILE, TABLE_ROW_ID, TABLE_ROW_TIMESTAMP, TABLE_ROW_OPERADORA, TABLE_ROW_REDE, TABLE_ROW_RSSI, TABLE_ROW_LATITUDE, TABLE_ROW_LONGITUDE);
            db.execSQL(newTableQueryString);

        }

        // This method only runs when we increment DB_VERSION
        // We will look at this in chapter 26
        @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion, int newVersion) {
        }
    }
}
