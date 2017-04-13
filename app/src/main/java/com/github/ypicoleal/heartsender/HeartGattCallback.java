package com.github.ypicoleal.heartsender;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class HeartGattCallback extends BluetoothGattCallback {
    static final int STATUS_CONNECTING = 1;
    static final int STATUS_DISCOVERING = 2;
    static final int STATUS_CONNECTED = 3;
    static final int STATUS_DISCONNECTED = 4;
    static final int BATTERY_READDING = -1;
    static final int BATTERY_UNKNOWN = -2;

    private final UUID HEART_RATE = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    private final UUID BODY_SENSOR = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
    private final UUID BATTERY_LEVEL = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    private OnDeviceCallback mDeviceCallback;

    HeartGattCallback(OnDeviceCallback deviceCallback) {
        super();
        mDeviceCallback = deviceCallback;
    }

    private void enableNotifications(BluetoothGatt gatt) {
        Log.i("gatt", "enableNotifications");

        gatt.readRemoteRssi();
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            Log.i("gatt", "Service UUID: " + service.getUuid());
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                Log.i("gatt", "Characteristic UUID: " + characteristic.getUuid());
                if (characteristic.getUuid().compareTo(HEART_RATE) == 0 || characteristic.getUuid().compareTo(BATTERY_LEVEL) == 0) {
                    Log.i("gatt", "you are the chosen one");
                    gatt.setCharacteristicNotification(characteristic, true);
                    gatt.readCharacteristic(characteristic);
                    mDeviceCallback.onBatteryLevelChange(BATTERY_READDING);
                } else if (characteristic.getUuid().compareTo(BODY_SENSOR) == 0) {
                    gatt.readCharacteristic(characteristic);
                }
                //
            }
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Log.i("gatt", "Status change: " + status + " " + newState);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            gatt.discoverServices();
            mDeviceCallback.onDeviceStatusChange(STATUS_DISCOVERING);
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            mDeviceCallback.onDeviceStatusChange(STATUS_DISCONNECTED);
            mDeviceCallback.onBatteryLevelChange(BATTERY_UNKNOWN);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.w("Gatt", "onServicesDiscovered received: " + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            enableNotifications(gatt);
            mDeviceCallback.onDeviceStatusChange(STATUS_CONNECTED);
        } else {
            Log.w("Gatt", "error onServicesDiscovered received: " + status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (characteristic.getUuid().compareTo(HEART_RATE) == 0) {
                mDeviceCallback.onHeartRateChange(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1));
            } else if (characteristic.getUuid().compareTo(BATTERY_LEVEL) == 0) {
                mDeviceCallback.onBatteryLevelChange(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
            } else {
                mDeviceCallback.onSensorLocationChange(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
            }

        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.i("gatt", "onCharacteristicWrite status: " + status);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (characteristic.getUuid().compareTo(HEART_RATE) == 0) {
            mDeviceCallback.onHeartRateChange(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1));
        } else if (characteristic.getUuid().compareTo(BATTERY_LEVEL) == 0) {
            mDeviceCallback.onBatteryLevelChange(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        Log.i("gatt", "onDescriptorRead: ");
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.i("gatt", "onDescriptorWrite: ");
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        Log.i("gatt", "onDescriptorWrite: ");
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        Log.i("gatt", "onReadRemoteRssi: ");
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        Log.i("gatt", "onMtuChanged: ");
    }

    interface OnDeviceCallback {
        void onBatteryLevelChange(int batteryLevel);

        void onDeviceStatusChange(int status);

        void onHeartRateChange(int heartRate);

        void onSensorLocationChange(int sensorLocation);
    }
}
