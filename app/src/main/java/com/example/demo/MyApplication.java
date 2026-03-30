package com.example.demo;

import android.app.Application;

import com.example.demo.network.NetWorkManager;

import org.litepal.LitePal;

import java.io.DataOutputStream;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LitePal.initialize(this);
        NetWorkManager.getInstance().init();
        grantAutoStart();
    }

    // 执行ROOT命令，把APP加入系统自启白名单
    private void grantAutoStart() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            // 加入自启
            os.writeBytes("pm grant " + getPackageName() + " android.permission.RECEIVE_BOOT_COMPLETED\n");
            // 加入后台保活
            os.writeBytes("settings put global dontkillapplication " + getPackageName() + "\n");
            os.writeBytes("exit\n");
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
