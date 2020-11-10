package com.example.joelwasserman.androidbletutorial;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.UUID;

public class BLE_Advert extends Service {
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeAdvertiser btAdvertiser;
    public static boolean running = false;
    public BLE_Advert() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btAdvertiser = btAdapter.getBluetoothLeAdvertiser();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true ;
        ParcelUuid pUuid = new ParcelUuid(UUID.fromString("00001803-0000-1000-8000-00805F9B34FB"));
        final AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_LOW )
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_POWER )
                .setConnectable(false)
                .build();
        final AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid( pUuid )
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceData( pUuid, new byte[]{1,2} )
                .build();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btAdvertiser.startAdvertising(settings,data,leAdvertiseCallback);
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }
    private AdvertiseCallback leAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i("BLE_ADVERT_SERVICE", "Mode: " + settingsInEffect.getMode() + " txPower: " + settingsInEffect.getTxPowerLevel() + "\n");
            sendBroadcastMessage("Started " + settingsInEffect.getTxPowerLevel());
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e("BLE_SERVICE", "Error:" + errorCode + "\n");
            sendBroadcastMessage("Failed " + errorCode);
        }
    };
    private void sendBroadcastMessage(String status) {
        Intent intent = new Intent();
        intent.setAction("com.example.joelwasserman.androidbletutorial");
        intent.putExtra("Status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        btAdvertiser.stopAdvertising(leAdvertiseCallback);
        running = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}