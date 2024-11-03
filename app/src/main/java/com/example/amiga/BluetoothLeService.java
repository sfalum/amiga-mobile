package com.example.amiga;

import android.annotation.SuppressLint;
import android.app.Service;

import android.bluetooth.BluetoothGattCharacteristic;

import android.content.Intent;

import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.WriteType;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;
@SuppressLint("MissingPermission")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();


    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_MTU_CHANGED =
            "com.example.bluetooth.le.ACTION_MTU_CHANGED";
    public final static String ACTION_NOTIFY_ENABLED =
            "com.example.bluetooth.le.ACTION_NOTIFY_ENABLED";
    public final static String ACTION_DATA_RECEIVED =
            "com.example.bluetooth.le.ACTION_DATA_RECEIVED";

    public final static String ACTION_ERROR =
            "com.example.bluetooth.le.ACTION_ERROR";
    public final static String EXTRA_DATA_VALUE =
            "com.example.bluetooth.le.EXTRA_DATA_VALUE";





    private final static String SERVICE = "e95d0753-251d-470a-a062-fa1922dfa9a8";
    private final static String NOTIFY  = "e95dca4b-251d-470a-a062-fa1922dfa9a8";
    private final static String READ_WRITE  = "e95dfb24-251d-470a-a062-fa1922dfa9a8";

    private final static  UUID SERVICE_UUID = UUID.fromString(SERVICE);
    private final static  UUID NOTIFY_CHARACTERISTICS_UUID = UUID.fromString(NOTIFY);
    private final static  UUID WR_CHARACTERISTICS_UUID = UUID.fromString(READ_WRITE);

    public BluetoothCentralManager central;
    private BluetoothPeripheral mBluetoothPeriperal;
    BluetoothGattCharacteristic writeCharacteristics;
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback(){

        @Override
        public void onServicesDiscovered(@NonNull BluetoothPeripheral peripheral) {
            Log.d(TAG, "onServicesDiscovered: ");
            mBluetoothPeriperal = peripheral;
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);
            writeCharacteristics  = peripheral.getCharacteristic(SERVICE_UUID, WR_CHARACTERISTICS_UUID);
            peripheral.readCharacteristic(writeCharacteristics);
            setDeviceNotification(peripheral,SERVICE_UUID,NOTIFY_CHARACTERISTICS_UUID,true);
              }

        @Override
        public void onMtuChanged(@NonNull BluetoothPeripheral peripheral, int mtu, @NonNull GattStatus status) {

        }

        @Override
        public void onNotificationStateUpdate(@NonNull BluetoothPeripheral peripheral, @NonNull BluetoothGattCharacteristic characteristic, @NonNull GattStatus status) {
            Log.d(TAG, "onNotificationStateUpdate: ");
            broadcastUpdate(ACTION_NOTIFY_ENABLED);
        }

        @Override
        public void onCharacteristicUpdate(@NonNull BluetoothPeripheral peripheral, @NonNull byte[] value, @NonNull BluetoothGattCharacteristic characteristic, @NonNull GattStatus status) {
            broadcastUpdate(ACTION_DATA_RECEIVED,value);
//            byte[] bytesX = { value[0],value[1],value[2],value[3]};
//            float fX = ByteBuffer.wrap(bytesX).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//
//            byte[] bytesY = { value[4],value[5],value[6],value[7]};
//            float fY = ByteBuffer.wrap(bytesY).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//
//            byte[] bytesZ = { value[8],value[9],value[10],value[11]};
//            float fZ = ByteBuffer.wrap(bytesZ).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//            Log.d(TAG, "onCharacteristicUpdate: x "+fX+", y "+fY+", z "+fZ);


        }
    };

    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            Log.d(TAG, "CONNECTED !");
            Log.d(TAG, "Attempting to start service discovery:" );
            Log.d(TAG, "onConnectedPeripheral: "+peripheral.getName());
            mConnectionState = STATE_CONNECTED;
            mBluetoothPeriperal = peripheral;
            broadcastUpdate(ACTION_GATT_CONNECTED);
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            Log.d(TAG, "onDisconnectedPeripheral: ");
            Log.d(TAG, "Disconnected from GATT server.");
            mConnectionState = STATE_DISCONNECTED;
            broadcastUpdate(ACTION_GATT_DISCONNECTED);
        }
    };


    private void broadcastUpdate(final String action) {
        Log.d(TAG, "broadcastUpdate: STRING");
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,final byte[] value) {
      //  Log.d(TAG, "broadcastUpdate: "+ Arrays.toString(value));
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_VALUE, value);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    public boolean initialize() {
        Log.d(TAG, "initialize: ");
        try{
            central = new BluetoothCentralManager(this, bluetoothCentralManagerCallback, new Handler());
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public boolean connect(final String address) {
        BluetoothPeripheral peripheral = central.getPeripheral(address);
        central.autoConnectPeripheral(peripheral,peripheralCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    public void disconnect() {
        Log.d(TAG, "disconnect: ");
        central.cancelConnection(mBluetoothPeriperal);
    }

    public void close() {
        Log.d(TAG, "close: ");
        central.close();
    }


    public void setDeviceNotification(BluetoothPeripheral peripheral, UUID SERVICE_UUID,UUID CHAR_UUID,
                                      boolean enabled) {
        BluetoothGattCharacteristic notifyCharacteristics = peripheral.getCharacteristic(SERVICE_UUID, CHAR_UUID);
        if (notifyCharacteristics != null) {
            peripheral.setNotify(notifyCharacteristics, enabled);
        }

    }


    public void writetoDevice(BluetoothPeripheral peripheral,byte[] data,UUID SERVICE_UUID,UUID CHAR_UUID)
    {
        Log.d(TAG, "writeCharacteristic: "+SERVICE_UUID+CHAR_UUID);
        Log.d(TAG, "writeData: "+ Arrays.toString(data));
        if (peripheral == null) {
            System.out.println(" is null.");
        } else {
            System.out.println(" not null.");
            peripheral.writeCharacteristic(writeCharacteristics,data,WriteType.WITH_RESPONSE);
        }

    }


    public void sendData(byte[] data){
        writetoDevice(mBluetoothPeriperal,data,
                SERVICE_UUID,
                WR_CHARACTERISTICS_UUID);
    };
}
