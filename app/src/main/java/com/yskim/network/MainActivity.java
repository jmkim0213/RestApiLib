package com.yskim.network;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yskim.network.http.HttpConnector;
import com.yskim.network.http.InspectionException;
import com.yskim.network.http.rest.RestApiRequestListener;
import com.yskim.network.http.rest.RestApiResponseListener;
import com.yskim.network.http.rest.RestApiService;
import com.yskim.network.http.rest.RestRequest;
import com.yskim.network.http.rest.RestResponse;
import com.yskim.utils.Logger;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SAMPLE = 0x01;


    private RestApiService mApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApiService = new RestApiService(this);


        Object obj = "obj";


        RestRequest restRequest = new RestRequest("http://yskim.com", HttpConnector.Method.GET);

        mApiService.requestAsync(
                REQUEST_SAMPLE,                 // 요청코드
                restRequest,                    // 요청
                RestResponse.class,             // 응답
                mRestApiRequestListener,        // 요청 이벤트 리스너
                mRestApiResponseListener,       // 응답 이벤트 리스너
                obj);                           // 범용 오브젝트
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mApiService.cancelAllRequest();
    }

    // 요청 이벤트 리스너
    private RestApiRequestListener mRestApiRequestListener = new RestApiRequestListener() {
        @Override
        public void onAsyncRequestStart() {
            super.onAsyncRequestStart();
            Logger.d("onAsyncRequestStart");
        }

        @Override
        public void onAsyncRequestFinish() {
            super.onAsyncRequestFinish();
            Logger.d("onAsyncRequestFinish");
        }
    };

    // 응답 이벤트 리스너
    private RestApiResponseListener mRestApiResponseListener = new RestApiResponseListener() {
        @Override
        public void onResponse(int requestTag, RestResponse response, Object obj) {
            super.onResponse(requestTag, response, obj);
            Logger.d("onResponse");

        }

        @Override
        public void onError(int requestTag, Exception e, Object obj) {
            super.onError(requestTag, e, obj);
            Logger.d("onError");
        }

        @Override
        public void onCanceled(int requestTag, Object obj) {
            super.onCanceled(requestTag, obj);
            Logger.d("onCanceled");
        }

        @Override
        public void onInspection(int requestTag, InspectionException e, Object obj) {
            super.onInspection(requestTag, e, obj);
            Logger.d("onInspection");
        }
    };
}
