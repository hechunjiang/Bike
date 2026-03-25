package com.example.demo;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 接口定义
 *
 * @author hcj
 * @since 2020-05-04
 */
public interface ApiRequest {
    @POST("/pEmissionBicycle")
    Observable<Object> pEmissionBicycle(@Body Map<String, Object> map);

}
