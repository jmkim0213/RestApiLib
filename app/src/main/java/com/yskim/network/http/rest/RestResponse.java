package com.yskim.network.http.rest;


import org.json.JSONObject;

/**
 * 서버 응답에 대한 응답 클래스
 * 공통적인 응답 데이터를 파싱하고 반환한다.
 * 다른 응답클래스들은 이 클래스를 상속해서 구현한다.
 *
 */
public class RestResponse {
	private final int mStatusCode;

	public RestResponse(Integer statusCode, JSONObject jsonObject) {
		mStatusCode = statusCode;
	}

	public int getStatusCode() {
		return mStatusCode;
	}
	
}
