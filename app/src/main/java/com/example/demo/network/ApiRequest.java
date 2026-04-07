package com.example.demo.network;

import com.example.demo.data.BiKeResponseData;
import com.example.demo.data.GetCountResponse;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 接口定义
 *
 * @author hcj
 * @since 2020-05-04
 */
public interface ApiRequest {
    @POST("/carbonProperty/pEmissionBicycle")
    Observable<Object> pEmissionBicycle(@Body BikeRequestBean map);

    @GET("/carbonProperty/pEmissionBicycle/count")
    Observable<GetCountResponse<BiKeResponseData>> getBikeCount(@Query("bakeId") String bakeId);

}
