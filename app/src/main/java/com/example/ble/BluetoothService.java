package com.example.ble;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothService extends Service {
    BluetoothAdapter my_bluetooth;
    BluetoothGatt bluetoothGatt1;
    BluetoothGatt bluetoothGatt2;
    BluetoothGattCharacteristic interval1;
    BluetoothGattCharacteristic interval2;
    BluetoothGattCharacteristic characteristic2;
    BluetoothGattCharacteristic stringCharacteristic2;
    BluetoothGattCharacteristic characteristic1;
    BluetoothGattCharacteristic stringCharacteristic1;
    private String deviceAddress1 = "60:2B:A8:76:25:47";
    //    private String deviceAddress1;
    boolean intervalset2;
    boolean intervalset1;
    private String deviceAddress2 = "47:75:AC:4E:78:36";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    // device -2
    public void connectToDevice2() {
        BluetoothDevice device = my_bluetooth.getRemoteDevice(deviceAddress2);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt2 = device.connectGatt(this, false, mygattCallback2);
    }

    private final BluetoothGattCallback mygattCallback2 = new BluetoothGattCallback() {//once device get connected to app, callback fuctions will call for each state
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice device = gatt.getDevice();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("conncetion", "connected successfully");
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("conncetion", "disconnceted");
                gatt.close();
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214"));
                characteristic2 = service.getCharacteristic(UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"));
                stringCharacteristic2 = service.getCharacteristic(UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214"));
                interval2 = service.getCharacteristic(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"));
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (characteristic2 != null && stringCharacteristic2 != null) {
                    long time = System.currentTimeMillis();
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putLong(time);
                    byte[] dataToSend = buffer.array();
                    stringCharacteristic2.setValue(dataToSend);
                    gatt.writeCharacteristic(stringCharacteristic2);
                } else {
                    Log.e("characteristci", "receives null value");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if(value.length==12){
                byte[] epoch = Arrays.copyOfRange(value,0,8);
                byte[] floatValueBytes = Arrays.copyOfRange(value,8,12);
                ByteBuffer epochBuffer = ByteBuffer.wrap(epoch).order(ByteOrder.LITTLE_ENDIAN);
                long epochValue = epochBuffer.getLong();
                float floatValue = ByteBuffer.wrap(floatValueBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                long milli = epochValue;
                long time = System.currentTimeMillis();
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(time);
                byte[] dataToSend = buffer.array();
                stringCharacteristic2.setValue(dataToSend);
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.writeCharacteristic(stringCharacteristic2);

            }
            if(value.length == 1){
                System.out.println("received");
            }
            else{
                Log.e("received value", "invalid bytes array");
                System.out.println(value.length);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "String characteristic written successfully.");
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.setCharacteristicNotification(characteristic2, true);
//                 Log.e("read", "ready to function");
                BluetoothGattDescriptor descriptor = characteristic2.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                BluetoothGattDescriptor descriptor2 = interval2.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor2);
                if (characteristic.getUuid().equals(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"))) {
                    Log.i(TAG, "Interval characteristic written successfully.");

                } else {
                    Log.e("Characteristic Write", "Failed with status: " + status);
                }
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor written successfully, notifications enabled.");
            } else {
                Log.e(TAG, "Descriptor write failed with status: " + status);
            }
        }
    };
    private void connectToDevice1() {
        BluetoothDevice device = my_bluetooth.getRemoteDevice(deviceAddress1);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt1 = device.connectGatt(this, false, mygattCallback1);
        // connect to device
    }
    private final BluetoothGattCallback mygattCallback1 = new BluetoothGattCallback() {
        //once device get connected to app, callback fuctions will call for each state
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // call`s when state of connection change CONNECT AND DISCONNECT
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice device = gatt.getDevice();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("conncetion", "connected successfully");
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("conncetion", "disconnceted");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //call`s when required service is discovered
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214"));
                characteristic1 = service.getCharacteristic(UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"));
                stringCharacteristic1 = service.getCharacteristic(UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214"));
                interval1 = service.getCharacteristic(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"));
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (characteristic1 != null && stringCharacteristic1 != null) {
                    long time = System.currentTimeMillis();
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putLong(time);
                    byte[] dataToSend = buffer.array();
                    stringCharacteristic1.setValue(dataToSend);
                    gatt.writeCharacteristic(stringCharacteristic1);

                } else {
                    Log.e("characteristci", "receives null value");
                }
            }
        }
        //call`s when data received from ble device
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if(value.length==12){
                byte[] epoch = Arrays.copyOfRange(value,0,8);
                byte[] floatValueBytes = Arrays.copyOfRange(value,8,12);
                ByteBuffer epochBuffer = ByteBuffer.wrap(epoch).order(ByteOrder.LITTLE_ENDIAN);
                long epochValue = epochBuffer.getLong();
                float floatValue = ByteBuffer.wrap(floatValueBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                long milli = epochValue;
                long time = System.currentTimeMillis();
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(time);
                byte[] dataToSend = buffer.array();
                stringCharacteristic1.setValue(dataToSend);
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.writeCharacteristic(stringCharacteristic1);

            }
            if(value.length == 1){
                byte[] ackData = characteristic.getValue();
                if (ackData != null && ackData.length > 0) {
                    if (ackData[0] == 0x01) { // 0x01 is the acknowledgment code sent by the Arduino
                        Log.i(TAG, "Acknowledgment received for interval update.");
                        Toast.makeText(getApplicationContext(), "Interval update acknowledged!", Toast.LENGTH_SHORT).show();
                    }
                }
                System.out.println("received");
            }
            else{
                Log.e("received value", "invalid bytes array");
                System.out.println(value.length);
            }
        }
        //call`s when data writtern to ble device
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "String characteristic written successfully.");
                if (ActivityCompat.checkSelfPermission(BluetoothService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.setCharacteristicNotification(characteristic1, true);
                gatt.setCharacteristicNotification(interval1, true);
//                Log.e("read", "ready to function");
                BluetoothGattDescriptor descriptor = characteristic1.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                BluetoothGattDescriptor descriptor1 = interval1.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor1);
                if (characteristic.getUuid().equals(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"))) {
                    Log.i(TAG, "Interval characteristic written successfully.");

                } else {
                    Log.e("Characteristic Write", "Failed with status: " + status);
                }
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor written successfully, notifications enabled.");
            } else {
                Log.e(TAG, "Descriptor write failed with status: " + status);
            }
        }


    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
