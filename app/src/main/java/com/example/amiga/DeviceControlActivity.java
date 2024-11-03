package com.example.amiga;



import android.annotation.SuppressLint;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_AUTOCONNECT = "DEVICE_AUTOCONNECT";


    private TextView tvConnectionState;
    private TextView tvDataReceived;

    private TextView tvFrequency;


    SeekBar simpleSeekBar ;

    private String mDeviceName;
    private String mDeviceAddress;
    private Boolean mDeviceAutoConnect;
    private boolean mConnected = false;

    private BluetoothLeService mBluetoothLeService;

    LineChart lineChart ;

    LineDataSet dataSetX = new LineDataSet(new ArrayList<>(), "x");
    LineDataSet dataSetY = new LineDataSet(new ArrayList<>(), "y");
    LineDataSet dataSetZ = new LineDataSet(new ArrayList<>(), "z");

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.e(TAG, "Connected !!");
            if(mDeviceAutoConnect){mBluetoothLeService.connect(mDeviceAddress);};

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "Disconnected !!");
            mBluetoothLeService = null;
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action){
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    tvConnectionState.setText(R.string.connected);
                    invalidateOptionsMenu();
                    Toast.makeText(DeviceControlActivity.this, "Device Connected !", Toast.LENGTH_SHORT).show();
                    break;

                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;
                    String status = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_VALUE);

                    tvConnectionState.setText(R.string.disconnected);
                    invalidateOptionsMenu();
                    break;

                case  BluetoothLeService.ACTION_MTU_CHANGED:
                    Toast.makeText(DeviceControlActivity.this, "Device Ready !", Toast.LENGTH_SHORT).show();
                    break;

                case BluetoothLeService.ACTION_NOTIFY_ENABLED:
                    Toast.makeText(DeviceControlActivity.this, "Listening to Device !", Toast.LENGTH_SHORT).show();
                    break;

                case BluetoothLeService.ACTION_DATA_RECEIVED:
                    byte[] byteArray = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_VALUE);
                    if(byteArray.length>2){
                        byte[] bytesX = { byteArray[0],byteArray[1],byteArray[2],byteArray[3]};
                        byte[] bytesY = { byteArray[4],byteArray[5],byteArray[6],byteArray[7]};
                        byte[] bytesZ = { byteArray[8],byteArray[9],byteArray[10],byteArray[11]};
                        float valueX = bytesToFloat(bytesX);
                        float valueY = bytesToFloat(bytesY);
                        float valueZ = bytesToFloat(bytesZ);
                        tvDataReceived.setText("x "+valueX+", y "+valueY+", z "+valueZ);
                        addEntry(valueX,valueY,valueZ);
                    }else{
                        System.out.println(Arrays.toString(byteArray));
                        tvFrequency.setText(byteArray[0]+" Hz");
                    }

                    break;
            }
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);


        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceAutoConnect = intent.getBooleanExtra(EXTRAS_DEVICE_AUTOCONNECT,false);


        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);

        tvConnectionState = (TextView) findViewById(R.id.connection_state);

        tvDataReceived = (TextView) findViewById(R.id.textViewReceived);
        tvFrequency  = (TextView) findViewById(R.id.textViewFrequency);
        simpleSeekBar = (SeekBar) findViewById(R.id.seekBarFrequency);

        simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                tvFrequency.setText(progressChangedValue+" Hz");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
               // byte[] data = new byte[]{(byte) 0x01};
                 byte[] byteProgres = new byte[1];
                byteProgres[0] = (byte) progressChangedValue;
                Toast.makeText(DeviceControlActivity.this, "Setting Device Frequency to :" + progressChangedValue + "Hz",
                        Toast.LENGTH_SHORT).show();
                mBluetoothLeService.sendData(byteProgres);
            }
        });

        lineChart = findViewById(R.id.AccelerometerChart);
        dataSetX.setColor(Color.RED);
        dataSetY.setColor(Color.GREEN);
        dataSetZ.setColor(Color.BLUE);

        LineData lineData = new LineData(dataSetX, dataSetY, dataSetZ);
        lineChart.setData(lineData);

        getSupportActionBar().setTitle(mDeviceName);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int checkedId = item.getItemId();
        if (checkedId == R.id.menu_connect) {
            tvConnectionState.setText("Connecting ....");
            mBluetoothLeService.connect(mDeviceAddress);

        } else if (checkedId == R.id.menu_disconnect) {
            mBluetoothLeService.disconnect();
            final Intent intent = new Intent(DeviceControlActivity.this, MainActivity.class);
            Log.d(TAG, "onBackPressed: ");
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("InlinedApi")
    @Override
    protected void onResume() {
        super.onResume();

       registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter(),RECEIVER_EXPORTED);
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result= " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_MTU_CHANGED);
        intentFilter.addAction(BluetoothLeService.ACTION_NOTIFY_ENABLED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RECEIVED);
        return intentFilter;
    }

    private float bytesToFloat(byte[] value){
        byte[] bytes = { value[0],value[1],value[2],value[3]};
        return Math.round(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat() * 1000.0f) / 1000.0f;
    }

    private void addEntry(float valueX, float valueY, float valueZ) {
        LineData data = lineChart.getData();

        if (data != null) {
            ILineDataSet set1 = data.getDataSetByIndex(0);
            ILineDataSet set2 = data.getDataSetByIndex(1);
            ILineDataSet set3 = data.getDataSetByIndex(2);

            data.addEntry(new Entry(set1.getEntryCount(), valueX), 0);
            data.addEntry(new Entry(set2.getEntryCount(), valueY), 1);
            data.addEntry(new Entry(set3.getEntryCount(), valueZ), 2);

            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.setVisibleXRangeMaximum(20);
            lineChart.moveViewToX(data.getEntryCount());
        }
    }


}