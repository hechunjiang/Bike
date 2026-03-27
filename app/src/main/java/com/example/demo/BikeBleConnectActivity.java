package com.example.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.demo.data.BikeData;
import com.example.demo.network.BikeRequestBean;
import com.example.demo.network.NetWorkManager;
import com.example.demo.util.DeviceIdUtil;
import com.example.demo.util.LogToFileUtil;
import com.example.demo.util.NumberUtils;
import com.example.demo.view.GaugeView;
import com.example.demo.view.MotionCurveView;

import org.litepal.LitePal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 蓝牙连接活动
 */
public class BikeBleConnectActivity extends AppCompatActivity {
    private static final String TAG = "BikeBleConnectActivity";
    private SimpleDateFormat dateFormat;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    // 固定mac地址
    private static final String TARGET_MAC = "50:FB:19:43:38:9C";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;

    private TextView tvDateValue;
    private TextView tvBikeTime;
    private TextView tvWeek;
    private TextView tvTotalCo2;
    private TextView tvCal;
    private TextView tvUseTime;
    private TextView tvDistance;
    private TextView tvLaps;
    private TextView tvSteps;

    private MotionCurveView powerTotalCo2View;
    private MotionCurveView powerCalView;
    private MotionCurveView powerUseTimeView;
    private MotionCurveView powerDistance;
    private MotionCurveView powerStepView;
    private MotionCurveView tapView;
    private GaugeView gaugeView;

    private LinearLayout layoutConnected;
    private LinearLayout layoutDisconnected;

    // 除开今天的总里程
    private Runnable timeRunnable;
    private Handler handler = new Handler(Looper.getMainLooper());

    private Disposable disposable;


    // 后端需要数据
    private int count = 0;
    private float cal;
    private float speed;
    private String glNumber;
    private float useTime;

    // 当日骑行距离 单位米
    private float curTodayDistance;

    // 计算圈数
    private static final String DIVIDER = "0.909090909";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全屏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        // 隐藏标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.bike);
        tvDateValue = findViewById(R.id.tv_date_value);
        tvBikeTime = findViewById(R.id.tv_bike_time);
        tvWeek = findViewById(R.id.tv_week);
        layoutConnected = findViewById(R.id.layout_connected);
        layoutDisconnected = findViewById(R.id.layout_disconnected);
        tvTotalCo2 = findViewById(R.id.tv_total_co2);
        powerTotalCo2View = findViewById(R.id.power_total_co2_view);
        tvCal = findViewById(R.id.tv_cal);
        powerCalView = findViewById(R.id.power_cal_view);
        tvUseTime = findViewById(R.id.tv_use_time);
        powerUseTimeView = findViewById(R.id.power_use_time_view);
        powerDistance = findViewById(R.id.power_distance_view);
        tvDistance = findViewById(R.id.tv_distance);
        tapView = findViewById(R.id.power_lap_view);
        tvLaps = findViewById(R.id.tv_laps);
        powerStepView = findViewById(R.id.power_step_view);
        tvSteps = findViewById(R.id.tv_steps);
        gaugeView = findViewById(R.id.gauge_view);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 初始化速度为0
        gaugeView.setSpeed(0);

        // 次数
        List<BikeData> bikeDataList = LitePal.where("time = ?", dateFormat.format(new Date())).find(BikeData.class);
        if (bikeDataList != null && !bikeDataList.isEmpty()) {
            count = bikeDataList.size();
            tvBikeTime.setText(String.format("%s", count));
        } else {
            tvBikeTime.setText("0");
        }

        // 碳减排量
        tvTotalCo2.setText("0");
        powerTotalCo2View.clearData();
        // 消耗
        tvCal.setText("0");
        powerCalView.clearData();

        // 使用时间
        tvUseTime.setText("0");
        powerUseTimeView.clearData();

        // 距离
        tvDistance.setText("0");
        powerDistance.clearData();

        // 圈数
        tvLaps.setText("0");
        tapView.clearData();

        // 每公里消耗
        tvSteps.setText("0");
        powerStepView.clearData();


        // 初始化蓝牙
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        checkPermissionsAndScan();

        timeRunnable = new Runnable() {
            @Override
            public void run() {
                showTime();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timeRunnable);
    }

    /**
     * 显示实时时间
     */
    public void showTime() {
        //星期几
        SimpleDateFormat weekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        tvDateValue.setText(dateFormat.format(new Date()));
        tvWeek.setText(weekFormat.format(new Date()));
    }

    // 检查权限并开始扫描
    private void checkPermissionsAndScan() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            // 开始扫描连接
            startScanBleDevice();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startScanBleDevice();
            } else {
                Toast.makeText(this, "缺少必要权限，无法使用BLE功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ====================== 1. 扫描 BLE 设备 ======================
    @SuppressLint("MissingPermission")
    private void startScanBleDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        // 开始扫描
        bleScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                BluetoothDevice device = result.getDevice();
                String deviceMac = device.getAddress();
                // ====================== 关键：匹配你的设备 MAC ======================
                if (TARGET_MAC.equals(deviceMac)) {
                    Log.d("deviceMac:", "Mac地址：" + deviceMac);
                    Toast.makeText(BikeBleConnectActivity.this, "找到自行车设备，正在连接...", Toast.LENGTH_SHORT).show();
                    // 停止扫描
                    bleScanner.stopScan(this);
                    // ====================== 直接连接设备（不经过系统配对！） ======================
                    connectBleDevice(device);
                }
            }
        });
    }

    // ====================== 2. 连接 BLE 设备 ======================
    @SuppressLint("MissingPermission")
    private void connectBleDevice(BluetoothDevice device) {
        // 连接设备（false = 不自动重连）
        bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            // 连接状态改变（连接成功/断开）
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d("BlueTooth", "设备连接成功" + device.getAddress() + "-----" + device.getName());
                    // 连接成功后，搜索设备服务（获取数据用）
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layoutConnected.setVisibility(View.VISIBLE);
                            layoutDisconnected.setVisibility(View.GONE);
                        }
                    });
                    gatt.discoverServices();
                }
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d("BlueTooth", "设备蓝牙断连");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layoutConnected.setVisibility(View.GONE);
                            layoutDisconnected.setVisibility(View.VISIBLE);
                            powerTotalCo2View.clearData();
                            powerCalView.clearData();
                            powerDistance.clearData();
                            powerUseTimeView.clearData();
                            tapView.clearData();
                            powerStepView.clearData();
                            gaugeView.setSpeed(0);

                            // 断链一次存一次数据
                            BikeData bikeData = new BikeData();
                            bikeData.setTime(dateFormat.format(new Date()));
                            bikeData.setDistance(curTodayDistance + "");
                            boolean isSave = bikeData.save();
                            if (isSave) {
                                count++;
                                tvBikeTime.setText(String.format("%s", count));
                            }
                            Log.d(TAG, "数据保存:" + (isSave ? "成功" : "失败"));

                        }
                    });
                }
            }

            // 搜索到服务（设备的所有数据通道）
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    UUID serviceUUID = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
                    UUID charUUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
                    Log.d("Bluetooth1", "服务发现成功");
                    // 获取目标服务和特征值
                    BluetoothGattCharacteristic characteristic = gatt.getService(serviceUUID).getCharacteristic(charUUID);
                    if (characteristic == null) {
                        Log.e("Bluetooth1", "特征值不存在");
                        Toast.makeText(BikeBleConnectActivity.this, "特征值不存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 检查特征值是否支持通知/指示
                    int properties = characteristic.getProperties();
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0 &&
                            (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
                        Log.e("Bluetooth1", "该特征值不支持通知/指示");
                        Toast.makeText(BikeBleConnectActivity.this, "该特征值不支持通知/指示", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 第一步：设置特征值的通知开启
                    boolean enableNotify = gatt.setCharacteristicNotification(characteristic, true);
                    if (!enableNotify) {
                        Log.e("Bluetooth1", "设置通知失败");
                        Toast.makeText(BikeBleConnectActivity.this, "设置通知失败", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 第二步：写入Descriptor（关键！很多人漏掉这一步）
                    BluetoothGattDescriptor descriptor =
                            characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if (descriptor != null) {
                        // 根据设备支持的类型选择：NOTIFY 用 ENABLE_NOTIFICATION_VALUE，INDICATE 用 ENABLE_INDICATION_VALUE
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        // 写入Descriptor（必须异步等待结果）
                        gatt.writeDescriptor(descriptor);
                    } else {
                        Log.e("Bluetooth1", "找不到通知Descriptor");
                        Toast.makeText(BikeBleConnectActivity.this, "找不到通知Descriptor", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BikeBleConnectActivity.this, "gatt" + status, Toast.LENGTH_SHORT).show();
                }
            }

            // 收到设备数据（回调）
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                // 这里就是自行车实时数据
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 有数据连接显示
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layoutConnected.setVisibility(View.VISIBLE);
                                layoutDisconnected.setVisibility(View.GONE);
                            }
                        });

                        byte[] data = characteristic.getValue();
                        // 解析 data 即可得到速度、里程、电量等
                        Log.d(TAG, bytesToHex(data));
                        LogToFileUtil.d(BikeBleConnectActivity.this, "BikeBleConnectActivity", bytesToHex(data));

                        // 行驶距离
                        curTodayDistance = parseMotion(bytesToHex(data), 4);
                        // 碳排放
                        tvTotalCo2.setText(NumberUtils.stringMultiply(curTodayDistance + "", "0.08"));
                        powerTotalCo2View.addData(NumberUtils.stringMultiplyToFloat(curTodayDistance + "", "0.08"));

                        // 消耗
                        cal = parseMotion(bytesToHex(data), 3);
                        tvCal.setText(NumberUtils.stringDivideFloor(cal + "", "1000"));
                        powerCalView.addData(cal);

                        // 里程
                        tvDistance.setText(NumberUtils.stringDivideFloor(curTodayDistance + "", "1000"));
                        powerDistance.addData(NumberUtils.stringDivideToFloat(curTodayDistance + "", "1000"));

                        // 用时
                        useTime = parseMotion(bytesToHex(data), 1);
                        tvUseTime.setText(formatSeconds((int) useTime));
                        powerUseTimeView.addData(useTime);

                        // 速度
                        speed = parseMotion(bytesToHex(data), 5);
                        gaugeView.setSpeed(speed);
                        // 圈数
                        String qNumber = NumberUtils.stringMultiply(curTodayDistance + "", DIVIDER);
                        tvLaps.setText(qNumber);
                        tapView.addData(Float.parseFloat(qNumber));

                        // 每公里消耗
                        glNumber = NumberUtils.stringDivideFloor(NumberUtils.stringDivideFloor(cal + "", "1000"), NumberUtils.stringDivideFloor(curTodayDistance + "", "1000"));
                        tvSteps.setText(glNumber);
                        powerStepView.addData(Float.parseFloat(glNumber));

                        sendBikeData();
                    }
                });
            }

            // 写入特征值回调
            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("Bluetooth", "数据发送成功！");
                } else {
                    Log.e("Bluetooth", "数据发送失败，状态码：" + status);
                }
            }
        });
    }

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
     * 将总秒数 转换为 00:00:00 格式
     *
     * @param totalSeconds 总秒数
     * @return 格式化后的时间字符串
     */
    public static String formatSeconds(int totalSeconds) {
        // 计算小时
        int hours = totalSeconds / 3600;
        // 计算分钟
        int minutes = (totalSeconds % 3600) / 60;
        // 计算秒
        int seconds = totalSeconds % 60;

        // %02d：不足2位自动补0，格式化输出
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
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

    public void sendBikeData() {
        disposable = Observable.interval(0, 60, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .flatMap((Function<Long, ObservableSource<Object>>) aLong -> {
                    // 发送数据
                    BikeRequestBean bikeRequestBean = new BikeRequestBean();
                    bikeRequestBean.setBicycleId(DeviceIdUtil.getAppUUID(this));
                    bikeRequestBean.setPower("23");
                    // 总里程
                    bikeRequestBean.setEmission(NumberUtils.stringDivideFloor(curTodayDistance + "", "1000"));
                    bikeRequestBean.setSpeed(speed + "");
                    bikeRequestBean.setDuration(useTime + "");
                    bikeRequestBean.setMileage(curTodayDistance + "");
                    bikeRequestBean.setCalories(cal + "");
                    return NetWorkManager.getApiRequest().pEmissionBicycle(bikeRequestBean)
                            .subscribeOn(Schedulers.io());
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {

                }, throwable -> {

                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 断开连接
        if (bluetoothGatt != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        if (disposable != null) {
            disposable.dispose();
        }
    }
}