package com.example.demo.util;

public interface BleCallBack {
    void backData(String data);

    void bleConnectSuccess();

    void bleReConnect();
}
