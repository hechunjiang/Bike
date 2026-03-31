package com.example.demo;

import android.Manifest;
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
import com.example.demo.util.BLEManager;
import com.example.demo.util.BleCallBack;
import com.example.demo.util.DeviceIdUtil;
import com.example.demo.util.LogToFileUtil;
import com.example.demo.util.NumberUtils;
import com.example.demo.view.GaugeView;
import com.example.demo.view.MotionCurveView;

import org.litepal.LitePal;
import org.litepal.crud.callback.FindMultiCallback;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
public class BikeBleConnectActivity extends AppCompatActivity implements BleCallBack {
    private static final String TAG = "BikeBleConnectActivity";
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat weekFormat;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    // 固定mac地址
//    private static final String TARGET_MAC = "50:FB:19:43:38:9C";
    private static final String TARGET_MAC = "50:FB:19:43:38:82";
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
    // 每多少分钟上送一次数据
    private static final int INTERVAL_60 = 60;
    // 数据发送 保证只启动一次定时器
    private boolean isCanSend = true;

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
        weekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

        // 次数 异步查询防止超时
        LitePal.where("time = ?", dateFormat.format(new Date())).findAsync(BikeData.class).listen(new FindMultiCallback<BikeData>() {
            @Override
            public void onFinish(List<BikeData> list) {
                if (list != null && !list.isEmpty()) {
                    count = list.size();
                    tvBikeTime.setText(String.format("%s", count));
                } else {
                    tvBikeTime.setText("0");
                }
            }
        });

        initData();

        // 初始化蓝牙
        checkPermissionsAndScan();

        timeRunnable = new Runnable() {
            @Override
            public void run() {
                tvDateValue.setText(dateFormat.format(new Date()));
                tvWeek.setText(weekFormat.format(new Date()));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timeRunnable);
        // 监听
        BLEManager.getInstance(this).setCallBack(this);
    }

    /**
     * 初始化数据
     */
    public void initData() {
        isCanSend = true;
        // 初始化速度为0
        gaugeView.setSpeed(0);

        // 碳减排量
        tvTotalCo2.setText("0");
        powerTotalCo2View.clearData();
        powerTotalCo2View.setMaxShowCount(5);

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
        tapView.setMaxShowCount(5);

        // 每公里消耗
        tvSteps.setText("0");
        powerStepView.clearData();
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
            BLEManager.getInstance(BikeBleConnectActivity.this).connect(TARGET_MAC);
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
                BLEManager.getInstance(BikeBleConnectActivity.this).connect(TARGET_MAC);
            } else {
                Toast.makeText(this, "缺少必要权限，无法使用BLE功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void backData(String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 有数据连接显示
                layoutConnected.setVisibility(View.VISIBLE);
                layoutDisconnected.setVisibility(View.GONE);
                // 解析 data 即可得到速度、里程、电量等
                LogToFileUtil.d(BikeBleConnectActivity.this, "BikeBleConnectActivity", data);
                // 行驶距离
                curTodayDistance = BLEManager.parseMotion(data, 4);
                // 碳排放
                tvTotalCo2.setText(NumberUtils.stringMultiply(curTodayDistance + "", "0.09"));
                powerTotalCo2View.addData(NumberUtils.stringMultiplyToFloat(curTodayDistance + "", "0.09"));

                // 消耗
                cal = BLEManager.parseMotion(data, 3);
                tvCal.setText(NumberUtils.stringDivideFloor(cal + "", "1000"));
                powerCalView.addData(cal);

                // 里程
                tvDistance.setText(NumberUtils.stringDivideFloor(curTodayDistance + "", "1000"));
                powerDistance.addData(NumberUtils.stringDivideToFloat(curTodayDistance + "", "1000"));

                // 用时
                useTime = BLEManager.parseMotion(data, 1);
                tvUseTime.setText(NumberUtils.formatSeconds((int) useTime));
                powerUseTimeView.addData(useTime);

                // 速度
                speed = BLEManager.parseMotion(data, 5);
                gaugeView.setSpeed(speed);
                // 圈数
                String qNumber = NumberUtils.stringMultiply(curTodayDistance + "", DIVIDER);
                tvLaps.setText(qNumber);
                tapView.addData(Float.parseFloat(qNumber));

                // 每公里消耗
                glNumber = NumberUtils.stringDivideFloor(NumberUtils.stringDivideFloor(cal + "", "1000"), NumberUtils.stringDivideFloor(curTodayDistance + "", "1000"));
                tvSteps.setText(glNumber);
                powerStepView.addData(Float.parseFloat(glNumber));
                if (isCanSend) {
                    isCanSend = false;
                    sendBikeData();
                }
            }
        });
    }

    @Override
    public void bleConnectSuccess() {
        runOnUiThread(() -> {
            layoutConnected.setVisibility(View.VISIBLE);
            layoutDisconnected.setVisibility(View.GONE);
        });
    }

    @Override
    public void bleReConnect() {
        runOnUiThread(() -> {
            layoutConnected.setVisibility(View.GONE);
            layoutDisconnected.setVisibility(View.VISIBLE);
            // 重置数据
            initData();
            BigDecimal number = new BigDecimal(curTodayDistance + "");
            // 当前保存成功 且当前距离不为0，保存成功置为0，防止重连后反复保存
            if (!NumberUtils.isNullOrZero(number)) {
                Log.d(TAG, "当次行驶距离:" + number);
                // 保存数据库
                BikeData bikeData = new BikeData();
                bikeData.setTime(dateFormat.format(new Date()));
                bikeData.setDistance(curTodayDistance + "");
                boolean isSave = bikeData.save();
                Log.d(TAG, "数据保存：" + (isSave ? "成功" : "失败"));
                count = count + 1;
                tvBikeTime.setText(String.format("%s", count));
                // 当前距离
                curTodayDistance = 0;
            } else {
                Log.d(TAG, "当次行驶距离为0 不保存数据");
            }
            if (disposable != null) {
                // 断链停止网络请求
                disposable.dispose();
            }
        });
    }

    /**
     * 发送数据保存到后端
     */
    public void sendBikeData() {
        disposable = Observable.interval(0, INTERVAL_60, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .flatMap((Function<Long, ObservableSource<Object>>) aLong -> {
                    // 发送数据
                    BikeRequestBean bikeRequestBean = new BikeRequestBean();
                    bikeRequestBean.setBicycleId(TARGET_MAC);
//                    bikeRequestBean.setPower("23");
                    bikeRequestBean.setCount(count + "");
                    // 碳排放量
                    bikeRequestBean.setEmission(NumberUtils.stringMultiply(curTodayDistance + "", "0.09"));
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
        // 蓝牙资源释放
        BLEManager.getInstance(this).release();
        // 网络释放
        if (disposable != null) {
            disposable.dispose();
        }
    }
}