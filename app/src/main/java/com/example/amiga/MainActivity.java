package com.example.amiga;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;

import android.bluetooth.le.ScanResult;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ListView lstvw;        //list view bluetooth devices
    private ArrayAdapter aAdapter;   // array adapter
   ArrayList list = new ArrayList();


    private BluetoothCentralManager central;
    Set<BluetoothPeripheral> scannedDevice = new HashSet<BluetoothPeripheral>();
    List<BluetoothPeripheral> ListViewDevice= new ArrayList<BluetoothPeripheral>();


    private static String[] ANDROID_12_PERMISSIONS= {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    private static String[] ANDROID_6_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            Log.d(TAG, "onDiscoveredPeripheral: " + peripheral.getName());
            scannedDevice.add(peripheral);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.btnGet);  //initialize button
        lstvw = (ListView) findViewById(R.id.deviceList); //initialize list view



        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        central = new BluetoothCentralManager(getApplicationContext(), bluetoothCentralManagerCallback, new Handler(Looper.getMainLooper()));

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                central.scanForPeripherals();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        central.stopScan();
                        list.clear();
                        ListViewDevice.clear();
                        for(BluetoothPeripheral device: scannedDevice){
                            if(device.getName().isEmpty()){

                            }else{
                                ListViewDevice.add(device);
                                String devicename = device.getName();
                                String macAddress = device.getAddress();
                                list.add("Name: "+devicename+"\nMAC Address: "+macAddress);
                            }
                        }

                        aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);  //create array adapter of paried divices
                        lstvw.setAdapter(aAdapter);  //set listview
                        scannedDevice.clear();
                    }
                }, 1000);
            }
        });


        lstvw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // if listview item clicked open activity and send MAC address of the selected devices
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                final Intent intent = new Intent(MainActivity.this, DeviceControlActivity.class);
                BluetoothPeripheral peripheral =  ListViewDevice.get(position);
                Log.d(TAG, "onItemClick: "+peripheral.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, peripheral.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, peripheral.getAddress());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_AUTOCONNECT, false);
                startActivity(intent);
             //   finish();
            }
        });
        checkPermissions();
    }


    private void checkPermissions(){
        int permissionLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        this,
                        ANDROID_12_PERMISSIONS,
                        1
                );
            }
        }else{
            if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        this,
                        ANDROID_6_PERMISSIONS,
                        1
                );
            }
        }
    }

}