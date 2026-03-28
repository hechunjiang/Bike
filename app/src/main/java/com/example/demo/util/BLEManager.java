package com.example.demo.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.UUID;

public class BLEManager {
    private static final String TAG = "BLEManager";
    private static final long RECONNECT_DELAY = 1500;

    private static BLEManager mInstance;
    private final Context mAppContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mTargetDevice;

    private boolean isAutoReconnect = true;
    private boolean isReconnecting = false;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // ====================== 你的设备 UUID（自己改） ======================
    public static final UUID SERVICE_UUID = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
    public static final UUID WRITE_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID NOTIFY_UUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mWriteChar;
    private BluetoothGattCharacteristic mNotifyChar;

    private BleCallBack callBack;

    // ====================== 单例 ======================
    private BLEManager(Context context) {
        mAppContext = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    public static synchronized BLEManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BLEManager(context);
        }
        return mInstance;
    }

    public void setCallBack(BleCallBack callBack) {
        this.callBack = callBack;
    }

    // ====================== 连接设备 ======================
    public void connect(String mac) {
        if (mac == null || mac.isEmpty()) return;
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) return;

        closeGatt();
        mTargetDevice = mBluetoothAdapter.getRemoteDevice(mac);
        mBluetoothGatt = mTargetDevice.connectGatt(mAppContext, false, gattCallback);
    }

    // ====================== GATT 回调 ======================
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.e(TAG, "连接成功");
                callBack.bleConnectSuccess();
                isReconnecting = false;
                gatt.discoverServices();

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.e(TAG, "断开连接 → 自动重连");
                callBack.bleReConnect();
                closeGatt();
                if (isAutoReconnect) startReConnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().equals(SERVICE_UUID)) {
                        mWriteChar = service.getCharacteristic(WRITE_UUID);
                        mNotifyChar = service.getCharacteristic(NOTIFY_UUID);

                        if (mNotifyChar != null) {
                            gatt.setCharacteristicNotification(mNotifyChar, true);
                        }
                        BluetoothGattDescriptor descriptor =
                                mNotifyChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        if (descriptor != null) {
                            // 根据设备支持的类型选择：NOTIFY 用 ENABLE_NOTIFICATION_VALUE，INDICATE 用 ENABLE_INDICATION_VALUE
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            // 写入Descriptor（必须异步等待结果）
                            gatt.writeDescriptor(descriptor);
                        } else {
                            Log.e("Bluetooth1", "找不到通知Descriptor");
                        }
                        break;
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            // 在这里接收设备发过来的数据
            callBack.backData(bytesToHex(data));
        }
    };

    // ====================== 发送数据 ======================
    public boolean send(byte[] data) {
        if (mBluetoothGatt == null || mWriteChar == null) return false;
        mWriteChar.setValue(data);
        return mBluetoothGatt.writeCharacteristic(mWriteChar);
    }

    // ====================== 自动重连 ======================
    private void startReConnect() {
        if (isReconnecting || mTargetDevice == null) return;
        isReconnecting = true;

        mMainHandler.postDelayed(() -> {
            if (mTargetDevice != null) {
                connect(mTargetDevice.getAddress());
            }
            isReconnecting = false;
        }, RECONNECT_DELAY);
    }

    // ====================== 关闭GATT ======================
    public void closeGatt() {
        if (mBluetoothGatt != null) {
            try {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
            } catch (Exception e) {
            }
            mBluetoothGatt = null;
        }
    }

    // ====================== 释放 ======================
    public void release() {
        isAutoReconnect = false;
        mMainHandler.removeCallbacksAndMessages(null);
        closeGatt();
        mTargetDevice = null;
    }

    // 数据解析

    /**
     * 将byte转换为16进制
     *
     * @param bytes 数据
     * @return 返回值
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 接收数据解析
     *
     * @param hexStr 16进制数据
     * @param type   哪种类型
     * @return
     */
    public static float parseMotion(String hexStr, int type) {
        if (type == 1) {
            // 运动多少秒
            int byte1 = Integer.parseInt(hexStr.substring(2, 4), 16);
            int byte2 = Integer.parseInt(hexStr.substring(4, 6), 16);
            return byte1 * 256 + byte2;
        }
        if (type == 2) {
            // 运动次数
            int byte1 = Integer.parseInt(hexStr.substring(6, 8), 16);
            int byte2 = Integer.parseInt(hexStr.substring(8, 10), 16);
            return byte1 * 256 + byte2;
        }
        if (type == 3) {
            // 卡路里
            int byte1 = Integer.parseInt(hexStr.substring(10, 12), 16);
            int byte2 = Integer.parseInt(hexStr.substring(12, 14), 16);
            int byte3 = Integer.parseInt(hexStr.substring(14, 16), 16);
            return (float) (byte1 * 256 + byte2 + byte3 * 0.01);
        }
        //002E 0000 00022A  0000A146 0000 00 00 00000000
        if (type == 4) {
            // 距离 米
            int byte1 = Integer.parseInt(hexStr.substring(16, 18), 16);
            int byte2 = Integer.parseInt(hexStr.substring(18, 20), 16);
            int byte3 = Integer.parseInt(hexStr.substring(20, 22), 16);
            int byte4 = Integer.parseInt(hexStr.substring(22, 24), 16);
            return (float) (byte1 * 65536 + byte2 * 256 + byte3 + byte4 * 0.01);
        }
        if (type == 5) {
            // 速度 KM/H
            int byte1 = Integer.parseInt(hexStr.substring(24, 26), 16);
            int byte2 = Integer.parseInt(hexStr.substring(26, 28), 16);
            return (float) (byte1 + byte2 * 0.01);
        }
        if (type == 6) {
            // 心率 次/分钟
            return Integer.parseInt(hexStr.substring(28, 30), 16);
        }
        if (type == 7) {
            // 阻力 48%
            return Integer.parseInt(hexStr.substring(30, 32), 16);
        }
        return 0.0f;
    }
}