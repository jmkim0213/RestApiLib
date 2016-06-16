package com.yskim.network.http.rest;

import com.yskim.network.http.HttpConnector;

import java.util.Map;

/**
 * 서버 요청에 대한 요청클래스
 * 공통적인 요청 데이터를 설정한다.
 * 다른 응답클래스들은 이 클래스를 상속해서 구현한다.
 *
 */
public class RestRequest {
    private String mUrl;
    private HttpConnector.Method mMethod;

    public RestRequest(String url, HttpConnector.Method method) {
        mUrl = url;
        mMethod = method;
    }

    public String getUrl() {
        return mUrl;
    }

    public HttpConnector.Method getMethod() {
        return mMethod;
    }

    public Map<String, String> toHeaderParameter() {
        return null;
    }

    public Map<String, String> toParameter() {
        return null;
    }

}
