package com.yskim.network.http.rest;

import com.yskim.network.http.InspectionException;

public abstract class RestApiResponseListener<Response extends RestResponse> {
	public void onResponse(int requestTag, Response response, Object obj) {}
	public void onError(int requestTag, Exception e, Object obj) {}
	public void onCanceled(int requestTag, Object obj) {}
	public void onInspection(int requestTag, InspectionException e, Object obj) {}
}
