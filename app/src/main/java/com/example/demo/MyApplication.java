package com.example.demo;

import android.app.Application;

import com.example.demo.network.NetWorkManager;

import org.litepal.LitePal;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LitePal.initialize(this);
        NetWorkManager.getInstance().init();
    }
}
