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
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.demo.network.BikeRequestBean;
import com.example.demo.network.NetWorkManager;
import com.example.demo.view.GaugeView;
import com.example.demo.view.MotionCurveView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BikeBleConnectActivity extends AppCompatActivity {
    private static final String TAG = "BikeBleConnectActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    // 固定mac地址
    private static final String TARGET_MAC = "50:FB:19:43:38:9C";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;

    private TextView tvDateValue;
    private TextView tvTime;
    private TextView tvWeek;
    private TextView tvDistance;
    private TextView tvCal;
    private TextView tvUseTime;
    private TextView tvPower;
    private TextView tvLaps;
    private MotionCurveView powerDistanceView;
    private MotionCurveView powerCalView;
    private MotionCurveView powerUseTimeView;
    private MotionCurveView powerView;

    private MotionCurveView tapView;

    private GaugeView gaugeView;
    private Runnable timeRunnable;

    private Runnable updateRunnable;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

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
        NetWorkManager.getInstance().init();
        tvDateValue = findViewById(R.id.tv_date_value);
        tvTime = findViewById(R.id.tv_time);
        tvWeek = findViewById(R.id.tv_week);

        tvDistance = findViewById(R.id.tv_distance);
        powerDistanceView = findViewById(R.id.power_distance_view);
        tvCal = findViewById(R.id.tv_cal);
        powerCalView = findViewById(R.id.power_cal_view);
        tvUseTime = findViewById(R.id.tv_use_time);
        powerUseTimeView = findViewById(R.id.power_use_time_view);
        powerView = findViewById(R.id.power_view);
        tvPower = findViewById(R.id.tv_power);
        tapView = findViewById(R.id.power_lap_view);
        tvLaps = findViewById(R.id.tv_laps);

        gaugeView = findViewById(R.id.gauge_view);

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

        /**   // 模拟每500ms更新一次功率数据
         updateRunnable = new Runnable() {
        @Override public void run() {
        // 生成200-250之间的随机功率（模拟图中平稳波动效果）
        float power = 20 + (random.nextFloat() * 10 - 3);
        powerDistanceView.addData(power);
        handler.postDelayed(this, 500); // 500ms后再次执行
        }
        };

         // 开始更新
         handler.post(updateRunnable);**/

    }

    /**
     * 显示实时时间
     */
    public void showTime() {
        // 日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        // 时间
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        //星期几
        SimpleDateFormat weekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

        tvDateValue.setText(dateFormat.format(new Date()));
        tvTime.setText(timeFormat.format(new Date()));
        tvWeek.setText(weekFormat.format(new Date()));
    }

    // 检查权限并开始扫描
    private void checkPermissionsAndScan() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                Log.d("deviceMac:", "Mac地址：" + deviceMac);
                // ====================== 关键：匹配你的设备 MAC ======================
                if (TARGET_MAC.equals(deviceMac)) {
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
                    gatt.discoverServices();
                }
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d("BlueTooth", "设备蓝牙断连");
                    gaugeView.setSpeed(0);
                }
            }

            // 搜索到服务（设备的所有数据通道）
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // 遍历所有服务 device确定 特征值 服务id就确认

                    /*for (BluetoothGattService service : gatt.getServices()) {
                        Log.d("Bluetooth", "服务UUID：" + service.getUuid().toString());
                        serviceUUID = UUID.fromString(service.getUuid().toString());
                        // 遍历当前服务下的所有特征值
                        for (BluetoothGattCharacteristic chara : service.getCharacteristics()) {
                            Log.d("Bluetooth", "  特征值UUID：" + chara.getUuid().toString());
                            charUUID = UUID.fromString(chara.getUuid().toString());
                            tvStic.setText(String.format("服务UUID:%s\n特征值UUID:%s", service.getUuid().toString(), chara.getUuid().toString()));
                            // 打印特征值属性（判断是否支持通知）
                            int properties = chara.getProperties();
                            String propStr = "";
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                propStr += "NOTIFY | ";
                            }
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                propStr += "INDICATE | ";
                            }
                            Log.d("Bluetooth", "  特征值属性：" + propStr);
                        }
                    }*/
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
                byte[] data = characteristic.getValue();
                // 解析 data 即可得到速度、里程、电量等
                Log.d(TAG, bytesToHex(data));
                LogToFileUtil.d(BikeBleConnectActivity.this, "BikeBleConnectActivity", bytesToHex(data));
                float distance = parseMotion(bytesToHex(data), 4);
                tvDistance.setText(String.format("%s", distance));
                powerDistanceView.addData(distance);
                float cal = parseMotion(bytesToHex(data), 3);
                tvCal.setText(String.format("%s", cal / 1000.0));
                float useTime = parseMotion(bytesToHex(data), 1);
                powerCalView.addData(cal);

                tvUseTime.setText(formatSeconds((int) useTime));
                powerUseTimeView.addData(useTime);

                // 速度
                float speed = parseMotion(bytesToHex(data), 5);
                gaugeView.setSpeed(speed);
                // 圈数
                String qNumber = divide(distance + "", "188.4");
                tvLaps.setText(qNumber);
                tapView.addData(Float.parseFloat(qNumber));
                String glNumber = divide(cal + "", distance + "");
                tvPower.setText(glNumber);
                powerView.addData(Float.parseFloat(glNumber));

                // 发送数据
                BikeRequestBean bikeRequestBean = new BikeRequestBean();
                bikeRequestBean.setBicycleId("11");
                bikeRequestBean.setPower("23");
                bikeRequestBean.setEmission("5");

                bikeRequestBean.setSpeed(speed + "");
                bikeRequestBean.setDuration(useTime + "");
                bikeRequestBean.setMileage(distance + "");
                bikeRequestBean.setCalories(cal + "");
                sendBikeData(bikeRequestBean);

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

    // ====================== 发送数据 ======================

    /**
     * 向蓝牙设备发送数据
     *
     * @param gatt          已连接的BluetoothGatt实例
     * @param serviceUUID   目标服务UUID
     * @param writeCharUUID 支持写入的特征值UUID
     * @param data          要发送的字节数组（注意：BLE单次发送数据长度通常≤20字节）
     * @return 是否成功发起写入请求
     */
    public boolean sendDataToDevice(BluetoothGatt gatt, UUID serviceUUID, UUID writeCharUUID, byte[] data) {
        // 1. 校验gatt连接状态
        if (gatt == null || data == null || data.length == 0) {
            Log.e("Bluetooth", "gatt为空或数据为空，发送失败");
            return false;
        }

        // 2. 获取目标服务和特征值
        BluetoothGattCharacteristic writeCharacteristic = gatt.getService(serviceUUID).getCharacteristic(writeCharUUID);
        if (writeCharacteristic == null) {
            Log.e("Bluetooth", "找不到写入特征值，检查UUID是否正确");
            return false;
        }

        // 3. 检查特征值是否支持写入
        int properties = writeCharacteristic.getProperties();
        boolean isWritable = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                || (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
        if (!isWritable) {
            Log.e("Bluetooth", "该特征值不支持写入");
            return false;
        }

        // 4. 设置要发送的数据（核心步骤）
        writeCharacteristic.setValue(data);

        // 5. 选择写入类型（根据特征值支持的属性）
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            // 无响应写入（设备无需回复，速度快，不保证送达）
            writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else {
            // 有响应写入（设备会回复写入结果，速度稍慢，保证送达）
            writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }

        // 6. 发起写入请求（异步操作，结果在onCharacteristicWrite回调中返回）
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
            return false;
        }
        boolean writeSuccess = gatt.writeCharacteristic(writeCharacteristic);
        if (writeSuccess) {
            Log.d("Bluetooth", "写入请求已发起，待设备响应");
        } else {
            Log.e("Bluetooth", "写入请求发起失败（可能gatt正忙）");
        }
        return writeSuccess;
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
     * 两个String类型数值相除，保留两位小数且向下取整
     *
     * @param dividendStr 被除数（String类型，如"10.5"）
     * @param divisorStr  除数（String类型，如"3"）
     * @return 向下取整后两位小数的String，异常时返回"0.00"
     */
    public static String divide(String dividendStr, String divisorStr) {
        // 1. 空值/空字符串处理
        if (dividendStr == null || dividendStr.trim().isEmpty()
                || divisorStr == null || divisorStr.trim().isEmpty()) {
            return "0.00";
        }

        double dividend;
        double divisor;
        try {
            // 2. String转Double（兼容整数/小数格式）
            dividend = Double.parseDouble(dividendStr.trim());
            divisor = Double.parseDouble(divisorStr.trim());

            // 3. 除数为0防护
            if (divisor == 0) {
                return "0.00";
            }

            // 4. 执行除法 + 向下取整保留两位小数
            double result = dividend / divisor;
            // 先×100向下取整，再÷100，保证两位小数严格向下取整
            double floorResult = Math.floor(result * 100) / 100;

            // 5. 格式化为两位小数的String（补零，如2.5→2.50）
            return String.format("%.2f", floorResult);

        } catch (NumberFormatException e) {
            // 非数字格式异常（如"abc"），返回默认值
            return "0.00";
        }
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

    public void sendBikeData(BikeRequestBean bikeRequestBean) {
        Disposable disposable = NetWorkManager.getApiRequest()
                .pEmissionBicycle(bikeRequestBean)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 断开连接
        if (bluetoothGatt != null && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }
}