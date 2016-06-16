package com.yskim.network.http.rest;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseArray;


import com.yskim.network.http.HttpConnector;
import com.yskim.network.http.InspectionException;
import com.yskim.utils.Logger;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 서버 API의 요청 인터페이스 클래스
 * 모든 API의 구현이 이 클래스를 상속하여 구현한다.
 * 공통파라미터 처리 및 공통 응답처리는 이 클래스에 구현되어있다. 
 *
 */
public class RestApiService {
	private Handler mHandler = new Handler(Looper.getMainLooper());
	private SparseArray<Thread> mHttpThreadMap;
	private Context mContext;

	public RestApiService(Context context) {
		mHttpThreadMap = new SparseArray<Thread>();
		mContext = context;
	}

	public RestResponse requestSync(String url,
									RestRequest request,
									Class<? extends RestResponse> responseClass,
									Object obj) throws InterruptedException, InspectionException, Exception {
		return requestSyncImp(request, responseClass, obj);
	}

	public void requestAsync(int requestTag,
							 RestRequest request,
							 Class<? extends RestResponse> responseClass,
							 RestApiRequestListener requestListener,
							 RestApiResponseListener<? extends RestResponse> responseListener,
							 Object obj) {

		requestAsyncImp(requestTag, request, responseClass, responseListener, requestListener, obj);
	}

	public void cancelRequest(int requestTag) {
		synchronized ( mHttpThreadMap ) {
			Thread httpThread = mHttpThreadMap.get(requestTag);
			if ( httpThread != null ) {
				httpThread.interrupt();
				mHttpThreadMap.remove(requestTag);
			}
		}
	}

	public void cancelAllRequest() {
		synchronized ( mHttpThreadMap ) {
			int mapSize = mHttpThreadMap.size();
			for ( int i = 0; i < mapSize; i++ ) {
				Thread thread = mHttpThreadMap.valueAt(i);
				if ( thread != null ) {
					thread.interrupt();
				}
			}
			mHttpThreadMap.clear();
		}
	}


	public Context getContext() {
		return mContext;
	}


	// TODO Private
	private HttpConnector generateConnector(int requestTag, RestRequest request) {
		String url = request.getUrl();
		HttpConnector.Method method = request.getMethod();

		HttpConnector connector = new HttpConnector(mContext, url, requestTag);
		connector.setMethod(method);


		// 파라미터 세팅.
		Map<String, String> headerParams = request.toHeaderParameter();
		if ( headerParams != null ) {
			Set<Entry<String, String>> headerParamSet = headerParams.entrySet();
			for ( Entry<String, String> paramEntry : headerParamSet ) {
				String key = paramEntry.getKey();
				String value = paramEntry.getValue();
				if ( !TextUtils.isEmpty(key) && !TextUtils.isEmpty(value) ) {
					connector.addHeaderParam(key, value);
				}
			}
		}
		Logger.d("headerParams: " + headerParams);

		Map<String, String> params = request.toParameter();
		if ( params != null ) {

			Set<Entry<String, String>> paramSet = params.entrySet();
			for ( Entry<String, String> paramEntry : paramSet ) {
				String key = paramEntry.getKey();
				String value = paramEntry.getValue();
				if ( !TextUtils.isEmpty(key) && !TextUtils.isEmpty(value) ) {
					connector.addParam(key, value);
				}
			}
		}

		Logger.d("params: " + params);

		return connector;
	}


	private RestResponse generateResponse(Integer statusCode, Class<? extends RestResponse> responseClass, String responseText, Object obj) throws Exception {
		Logger.e("Response: " + responseText);


		JSONObject responseObject = null;
		if ( !TextUtils.isEmpty(responseText) ) {
			responseObject = new JSONObject(responseText);
		}
		else {
			responseObject = new JSONObject();
		}



		Constructor<? extends RestResponse> responseConstructor = responseClass.getConstructor(statusCode.getClass(), responseObject.getClass());
		RestResponse response = responseConstructor.newInstance(statusCode, responseObject);

		return response;
	}

	private RestResponse requestSyncImp(RestRequest request,
										Class<? extends RestResponse> responseClass,
										Object obj) throws InterruptedException, InspectionException, Exception {

		HttpConnector connector = generateConnector(-1, request);

		String responseText = connector.sendRequestSync();
		int statusCode = connector.getStatusCode();


		RestResponse response = generateResponse(statusCode, responseClass, responseText, obj);


		return response;
	}

	private void requestAsyncImp(final int requestTag,
								 final RestRequest request,
								 final Class<? extends RestResponse> responseClass,
								 final RestApiResponseListener responseListener,
								 final RestApiRequestListener requestListener,
								 final Object obj) {

		cancelRequest(requestTag);


		final HttpConnector connector = generateConnector(requestTag, request);


		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				notifyAsyncRequestStart(requestListener);

				try {

					String responseText = connector.sendRequestSync();
					int statusCode = connector.getStatusCode();

					RestResponse response = generateResponse(statusCode, responseClass, responseText, obj);
					notifyResponse(responseListener, requestTag, response, obj);

				} catch (InterruptedException e) {
					Logger.e(" InterruptedException: " + e.toString());

					notifyCanceled(responseListener, requestTag, obj);

				} catch (InspectionException e) {
					Logger.e(" InspectionException: " + e.toString());

					notifyInspection(responseListener, requestTag, e, obj);

				} catch (Exception e) {
					Logger.e(" Exception: " + e.toString());
					notifyError(responseListener, requestTag, e, obj);

				}

				notifyAsyncRequestFinish(requestListener);
			}
		});

		synchronized ( mHttpThreadMap ) {
			mHttpThreadMap.put(requestTag, thread);
		}
		thread.start();
	}

	private void notifyResponse(final RestApiResponseListener listener, final int requestTag, final RestResponse response, final Object obj) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if ( listener != null ) {
					listener.onResponse(requestTag, response, obj);
				}
			}
		});
	}

	private void notifyError(final RestApiResponseListener listener, final int requestTag, final Exception e, final Object obj) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if ( listener != null ) {
					listener.onError(requestTag, e, obj);
				}
			}
		});
	}

	private void notifyCanceled(final RestApiResponseListener listener, final int requestTag, final Object obj) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if ( listener != null ) {
					listener.onCanceled(requestTag, obj);
				}
			}
		});
	}
	private void notifyInspection(final RestApiResponseListener listener, final int requestTag, final InspectionException e, final Object obj) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if ( listener != null ) {
					listener.onInspection(requestTag, e, obj);
				}
			}
		});
	}

	private void notifyAsyncRequestStart(final RestApiRequestListener listener) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if ( listener != null ) {
					listener.onAsyncRequestStart();
				}
			}
		});
	}

	private void notifyAsyncRequestFinish(final RestApiRequestListener listener) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if ( listener != null ) {
					listener.onAsyncRequestFinish();
				}
			}
		});
	}
}
