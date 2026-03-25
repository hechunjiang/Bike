package com.example.demo.network;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

/**
 * 接口定义
 *
 * @author hcj
 * @since 2020-05-04
 */
public interface ApiRequest {

    String HOST_URL_MAIN = "http://182.150.57.52:39527/";

    @POST("/carbonProperty/pEmissionBicycle")
    Observable<Object> pEmissionBicycle(@Body BikeRequestBean map);

}
