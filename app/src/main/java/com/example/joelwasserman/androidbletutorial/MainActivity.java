
package com.example.joelwasserman.androidbletutorial;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button startScanningButton;
    Button stopScanningButton;
    Button startAdvertisingButton;
    Button stopAdvertisingButton;
    TextView peripheralTextView;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Intent LE_scanning = new Intent(this, LE_Scan.class);
        final Intent LE_Advert = new Intent(this, BLE_Advert.class);
        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());


        if (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled() && !BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
            peripheralTextView.setText("ADVERTISING NOT SUPPORTED");
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int rssi = intent.getIntExtra("RSSI", 0);
                        CharSequence advStatus = intent.getCharSequenceExtra("Status");
                        if(advStatus != null){
                            peripheralTextView.append(advStatus + "\n");
                        }
                        if(rssi != 0) {
                            peripheralTextView.append(String.valueOf(rssi) + "\n");
                        }
                    }
                }, new IntentFilter("com.example.joelwasserman.androidbletutorial")
        );

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startService(LE_scanning);
                startScanningButton.setVisibility(View.INVISIBLE);
                stopScanningButton.setVisibility((View.VISIBLE));
                peripheralTextView.setText("");
            }
        });
        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(LE_scanning);
                startScanningButton.setVisibility((View.VISIBLE));
                stopScanningButton.setVisibility((View.INVISIBLE));
            }
        });
        startAdvertisingButton = (Button) findViewById(R.id.startAdvertise);
        startAdvertisingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startService(LE_Advert);
                startAdvertisingButton.setVisibility(View.INVISIBLE);
                stopAdvertisingButton.setVisibility((View.VISIBLE));
            }
        });
        stopAdvertisingButton = (Button) findViewById(R.id.stopAdvertising);
        stopAdvertisingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(LE_Advert);
                startAdvertisingButton.setVisibility(View.VISIBLE);
                stopAdvertisingButton.setVisibility((View.INVISIBLE));
            }
        });

        stopScanningButton.setVisibility(View.INVISIBLE);
        stopAdvertisingButton.setVisibility(View.INVISIBLE);


        if (BluetoothAdapter.getDefaultAdapter() != null && !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
        if(LE_Scan.running){
            startScanningButton.setVisibility(View.INVISIBLE);
            stopScanningButton.setVisibility(View.VISIBLE);
        }
        if(BLE_Advert.running){
            startAdvertisingButton.setVisibility(View.INVISIBLE);
            stopAdvertisingButton.setVisibility(View.VISIBLE);
        }
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }
}
