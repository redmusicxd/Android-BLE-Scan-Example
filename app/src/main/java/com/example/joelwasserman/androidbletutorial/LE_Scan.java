package com.example.joelwasserman.androidbletutorial;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LE_Scan extends Service {
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    public static boolean running = false;
    RequestQueue queue;
    public LE_Scan() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        queue = Volley.newRequestQueue(this);
        createNotification();
    }
    private void createNotification() {

        Notification notification = new NotificationCompat.Builder(this)

                .setContentTitle(getApplicationContext().getResources().getString(R.string.app_name))

                .setSmallIcon(R.mipmap.ic_launcher)

                .setOngoing(true)

                .setCategory(NotificationCompat.CATEGORY_SERVICE)

                .setPriority(Notification.PRIORITY_MIN)

                .build();

        startForeground(101, notification);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString("00001803-0000-1000-8000-00805F9B34FB"))
                .build();
        filters.add(filter);
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(filters,settings,leScanCallback);
                running = true;
                Log.i("BLE_SCAN_SERVICE","Started");
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }
    private void sendBroadcastMessage(int rssi) {
            Intent intent = new Intent();
            intent.setAction("com.example.joelwasserman.androidbletutorial");
            intent.putExtra("RSSI", rssi);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    // Device scan callback.
    private final ScanCallback leScanCallback = new ScanCallback() {
        String url = "http://10.0.0.150:3000/rssi";
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject("{\"device\": \"" + result.getDevice().getName() + "\",\"rssi\": " + result.getRssi() + "}"),
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    sendBroadcastMessage((int) response.get("rssi"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e("JSON_ERR", error.toString());
                            }
                });
                queue.add(stringRequest);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        btScanner.stopScan(leScanCallback);
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}