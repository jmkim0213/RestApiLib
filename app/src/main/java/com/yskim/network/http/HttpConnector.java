package com.yskim.network.http;

import android.content.Context;
import android.text.TextUtils;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.yskim.utils.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 
 * @author Kim Young-Soo
 * 
 */
public class HttpConnector {
	/**
	 * Http 메소드
	 */
	public static enum Method {
		POST, GET, PUT
	}

	private static final String CONTENT_TYPE = "application/json";
	private static final int TIME_LIMIT = 30 * 1000;
//	private static final int TIME_LIMIT = 5 * 1000;

	public static final int RESULT_COMPLETE = 0x01;
	public static final int RESULT_FAIL = 0x02;
	public static final int RESULT_CANCEL = 0x03;

	private int mRequestTag; // Request Tag

	private ArrayList<NameValuePair> mParams; // Request Params
	private HashMap<String, String> mFileParams;
	private HashMap<String, String> mHeaderParams;

	private String mUrl; // Request URL
	private Object mObject;
	private HttpConnectorDelegate mDelegate;
	private Thread mCurrentThread;

	private boolean mRequestFlag;

	private Method mMethod; // Default is Post

	private int mResult;
	private int mStatusCode;
	private Exception mException;

	private InputStreamEntity mPutInputStreamEntity;

	private Context mContext;

	public HttpConnector(HttpConnector connector) {
		mResult = -1;
		mStatusCode = -1;

		mParams = new ArrayList<NameValuePair>();
		mFileParams = new HashMap<String, String>();
		mHeaderParams = new HashMap<String, String>();

		mParams.addAll(connector.mParams);
		mFileParams.putAll(connector.mFileParams);
		mHeaderParams.putAll(connector.mHeaderParams);

		mRequestTag = connector.mRequestTag;
		mUrl = connector.mUrl;
		mMethod = connector.mMethod;

		mDelegate = connector.mDelegate;
		mObject = connector.mObject;

		mContext = connector.mContext;
		mPutInputStreamEntity = connector.mPutInputStreamEntity;
	}

	public HttpConnector(Context context, String url, int requestTag) {
		mRequestTag = requestTag;
		mUrl = url;
		mMethod = Method.POST;
		mResult = -1;
		mStatusCode = -1;

		mParams = new ArrayList<NameValuePair>();
		mFileParams = new HashMap<String, String>();
		mHeaderParams = new HashMap<String, String>();
		mContext = context;
	}

	// TODO PUBLIC
	public void sendRequestAsync() {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}

			mRequestFlag = true;

			mCurrentThread = new Thread(new HttpRunnable());
			mCurrentThread.start();
		}
	}

	public String sendRequestSync() throws InterruptedException, Exception {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}

			mRequestFlag = true;
			mCurrentThread = Thread.currentThread();
			return httpRequest();
		}
	}

	public void cancel() {
		synchronized (this) {
			if (mCurrentThread != null) {
				mCurrentThread.interrupt();
				mCurrentThread = null;
			}
		}
	}

	public void release() {
		cancel();

		if (mParams != null) {
			mParams.clear();
			mParams = null;
		}

		if (mHeaderParams != null) {
			mHeaderParams.clear();
			mHeaderParams = null;
		}

		mUrl = null;

		mObject = null;

		mDelegate = null;

		mCurrentThread = null;

		mMethod = null;

		mPutInputStreamEntity = null;
	}

	public void setPutEntity(InputStreamEntity is) {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}
			mPutInputStreamEntity = is;
		}
	}

	public void addParam(String key, String value) {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}
			mParams.add(new BasicNameValuePair(key, value));
		}
	}

	public void addFileParam(String key, String filePath) {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}
			mFileParams.put(key, filePath);
		}
	}

	public void addHeaderParam(String key, String value) {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}
			if (!mHeaderParams.containsKey(key)) {
				mHeaderParams.put(key, value);
			}
		}
	}

	/* * * * * * * * * *
	 * Setter & Getter * * * * * * * * *
	 */
	public void setDelegate(HttpConnectorDelegate delegate) {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}
			mDelegate = delegate;
		}
	}

	public HttpConnectorDelegate getDelegate() {
		return mDelegate;
	}

	public void setMethod(Method method) {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}
			mMethod = method;
		}
	}

	public void setObject(Object object) {
		synchronized (this) {
			if (mRequestFlag) {
				throw new IllegalStateException("already started request");
			}
			mObject = object;
		}
	}

	public Method getMethod() {
		return mMethod;
	}

	public Object getObject() {
		return mObject;
	}

	public String getRequestURL() {
		return mUrl;
	}

	public int getRequestTag() {
		return mRequestTag;
	}

	public int getResult() {
		return mResult;
	}

	public int getStatusCode() {
		return mStatusCode;
	}

	public Exception getException() {
		return mException;
	}

	public String getFullUrl() {
		StringBuilder urlButilder = new StringBuilder(2048);
		urlButilder.append(mUrl);
		urlButilder.append('?');
		for (NameValuePair paramPair : mParams) {
			urlButilder.append(paramPair.getName());
			urlButilder.append('=');
			try {
				urlButilder.append(URLEncoder.encode(paramPair.getValue(), "UTF-8"));
			} catch (Exception e) {
			}
			urlButilder.append('&');
		}
		urlButilder.deleteCharAt(urlButilder.length() - 1);

		return urlButilder.toString();
	}

	// TODO PRIVATE
	private void checkInterrupt() throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Request is Canceled");
		}
	}

	private void onCompleteRequest(HttpConnector connector, String response) {
		mResult = RESULT_COMPLETE;
		if (mDelegate != null) {
			mDelegate.onCompleteRequest(this, response);
		}
	}

	private void onFailedRequest(HttpConnector connector, Exception e) {
		mResult = RESULT_FAIL;
		if (mDelegate != null) {
			mDelegate.onFailedRequest(this, e);
		}
	}

	private void onCanceledRequest(HttpConnector connector) {
		mResult = RESULT_CANCEL;
		if (mDelegate != null) {
			mDelegate.onCanceledRequest(this);
		}
	}

	//
	private class HttpRunnable implements Runnable {
		public void run() {
			try {
				String response = httpRequest();
				onCompleteRequest(HttpConnector.this, response);
			} catch (InterruptedException e) {
				onCanceledRequest(HttpConnector.this);
			} catch (Exception e) {
				onFailedRequest(HttpConnector.this, e);
			}
		}
	};

	private String httpRequest() throws InterruptedException, Exception {
		HttpParams httpPrams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpPrams, TIME_LIMIT);
		HttpConnectionParams.setSoTimeout(httpPrams, TIME_LIMIT);
		
		HttpClient httpClient = getHttpClient();

		InputStream is = null;
		BufferedInputStream bis = null;
		try {
			checkInterrupt();

			HttpUriRequest httpUriRequest = null;
			String fullURL = getFullUrl();
			Logger.e("URL: " + fullURL);
			switch (mMethod) {
			case GET:
				HttpGet httpGet = new HttpGet(fullURL);
				httpUriRequest = httpGet;

				break;

			case POST:
				HttpPost httpPost = new HttpPost(mUrl);
				setPostParams(httpPost);
				httpUriRequest = httpPost;
				break;

			case PUT:
				HttpPut httpPut = new HttpPut(mUrl);
				setPostParams(httpPut);

				if (mPutInputStreamEntity != null) {
					httpPut.setEntity(mPutInputStreamEntity);
				}

				httpUriRequest = httpPut;
				break;
			}

			checkInterrupt();

			httpUriRequest.setHeader("Content-Type", CONTENT_TYPE);
			httpUriRequest.setHeader("User-Agent", "ANDROID");
			httpUriRequest.setHeader("Accept", CONTENT_TYPE);

			Set<String> keySet = mHeaderParams.keySet();
			for (String key : keySet) {
				String value = mHeaderParams.get(key);
				httpUriRequest.setHeader(key, value);
			}
			
			checkInterrupt();

			HttpResponse httpResponse = httpClient.execute(httpUriRequest);
			checkInterrupt();

			mStatusCode = httpResponse.getStatusLine().getStatusCode();
			Logger.e("StatusCode: " + mStatusCode);
			checkInterrupt();

			HttpEntity httpEntity = httpResponse.getEntity();
			Header[] headers = httpResponse.getHeaders("Accept-Encoding");

			Header acceptEncodingHeader = (headers != null && headers.length > 0) ? headers[0] : null;
			if (acceptEncodingHeader != null) {
				String acceptEncoding = acceptEncodingHeader.getValue();
				if (!TextUtils.isEmpty(acceptEncoding) && acceptEncoding.contains("gzip")) {
					String response = unzipResponse(httpEntity);

					return response;
				} else {
					BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(httpEntity);
					String response = EntityUtils.toString(bufHttpEntity, HTTP.UTF_8);

					return response;
				}
			} else {
				BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(httpEntity);
				InputStream contentStream = bufHttpEntity.getContent();
				try {
					CharsetMatch match = new CharsetDetector().setText(contentStream).detect();
					if (match != null) {
						String charset = match.getName();
						return EntityUtils.toString(bufHttpEntity, charset);
					}
				} catch (UnsupportedEncodingException ue) {

				} finally {
					contentStream.close();
				}

				return EntityUtils.toString(bufHttpEntity, "UTF-8");
			}

		} catch (InterruptedException e) {
			mException = e;
			throw e;

		} catch (Exception e) {
			mException = e;
			throw e;
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
				}
			}

			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}

			if (httpClient != null) {
				ClientConnectionManager connectionManager = httpClient.getConnectionManager();
				if (connectionManager != null) {
					connectionManager.shutdown();
				}
			}
		}
	}

	private void setPostParams(HttpEntityEnclosingRequestBase httpPost) {

		try {
			if (mFileParams.size() > 0) {

				MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, null);
				for (NameValuePair paramPair : mParams) {
					String paramName = paramPair.getName();
					String paramValue = paramPair.getValue();
					String encodedParamValue = URLEncoder.encode(paramValue, HTTP.UTF_8);

					StringBody stringBody = new StringBody(encodedParamValue);
					entity.addPart(paramName, stringBody);
				}

				Set<String> fileKeySet = mFileParams.keySet();
				for (String key : fileKeySet) {
					File file = new File(mFileParams.get(key));
					FileBody fileBody = new FileBody(file);
					entity.addPart(key, fileBody);
				}

				httpPost.setEntity(entity);
			} else {

				HashMap<String, String> paramsMap = new HashMap<String, String>(mParams.size());

				for (NameValuePair paramPair : mParams) {
					String paramName = paramPair.getName();
					String paramValue = paramPair.getValue();
					paramsMap.put(paramName, paramValue);
				}


				JSONObject paramObject = new JSONObject(paramsMap);

				ByteArrayEntity entity = new ByteArrayEntity(paramObject.toString().getBytes("UTF8"));
//				entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

				httpPost.setEntity(entity);
//				httpPost.setEntity(new UrlEncodedFormEntity(mParams));

			}



		} catch (UnsupportedEncodingException e) {

		}
	}

	private String unzipResponse(HttpEntity httpEntity) {
		StringBuilder responseBuilder = new StringBuilder(2048);
		byte[] buffer = new byte[2048];
		GZIPInputStream gzipInputStream = null;
		try {
			InputStream contentIs = httpEntity.getContent();
			gzipInputStream = new GZIPInputStream(contentIs);

			int readByte = 0;
			while ((readByte = gzipInputStream.read(buffer)) > 0) {
				String readString = new String(buffer, 0, readByte);
				responseBuilder.append(readString);
			}
		} catch (Exception e) {

		} finally {
			if (gzipInputStream != null) {
				try {
					gzipInputStream.close();
				} catch (IOException e) {
				}
			}
		}

		return responseBuilder.toString();
	}

	public static interface HttpConnectorDelegate {
		public void onCompleteRequest(HttpConnector connector, String response);

		public void onFailedRequest(HttpConnector connector, Exception e);

		public void onCanceledRequest(HttpConnector connector);
	}
	
    private HttpClient getHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new SFSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, TIME_LIMIT);
    		HttpConnectionParams.setSoTimeout(params, TIME_LIMIT);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

	private class SFSSLSocketFactory extends SSLSocketFactory {
	    SSLContext sslContext = SSLContext.getInstance("TLS");

	    public SFSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
	        super(truststore);

	        TrustManager tm = new X509TrustManager() {
	            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }


	        };

	        sslContext.init(null, new TrustManager[]{tm}, null);
//	        sslContext.init(null, new TrustManager[] { tm }, new SecureRandom());
	    }

	    @Override
	    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
	        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	    }

	    @Override
	    public Socket createSocket() throws IOException {
	        return sslContext.getSocketFactory().createSocket();
	    }
	}

}
