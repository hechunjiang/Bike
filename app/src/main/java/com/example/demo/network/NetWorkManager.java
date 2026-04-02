package com.example.demo.network;

import android.util.Log;

import com.example.demo.BikeBleConnectActivity;
import com.example.demo.network.converter.GoonConverterFactory;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;


/**
 * 网络请求
 *
 * @author hcj
 * @since 2020-05-04
 */
public class NetWorkManager {

    private static NetWorkManager mInstance;
    private static Retrofit retrofit;
    private static volatile ApiRequest apiRequest = null;

    public static NetWorkManager getInstance() {
        if (mInstance == null) {
            synchronized (NetWorkManager.class) {
                if (mInstance == null) {
                    mInstance = new NetWorkManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化必要对象和参数
     */
    public void init() {
        // 添加日志打印
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            try {
                String text = URLDecoder.decode(message, "utf-8");
                Log.d("NetWorkManager", text);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY);


        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(new MyBaseUrlInterceptor())
                .addInterceptor(loggingInterceptor)
                .build();

        // 初始化Retrofit
        retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(BikeBleConnectActivity.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GoonConverterFactory.create(new Gson()))
                .build();
    }

    public static ApiRequest getApiRequest() {
        if (apiRequest == null) {
            synchronized (ApiRequest.class) {
                apiRequest = retrofit.create(ApiRequest.class);
            }
        }
        return apiRequest;
    }

}
