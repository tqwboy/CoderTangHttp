package com.tang.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okio.BufferedSink;
import android.text.TextUtils;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.Response;
import com.tang.http.callback.HttpPostResponseCallback;
import com.tang.http.callback.HttpResponseCallback;

public class HttpManager {
	public static final int ERR_TYPE_IO = 0x0001;
	public static final int ERR_TYPE_NET = 0x0002;

	private static final String USER_AGENT = "Mozilla/5.0 Android "
			+ android.os.Build.VERSION.SDK_INT + " CoderTangHttp.jar";

	private OkHttpClient mClient = null;
	private ConcurrentHashMap<String, Call> mHttpCallMap = null;

	public HttpManager(int connectTimeout, int readTimeout, int writeTimeout) {
		mClient = new OkHttpClient();

		mClient.setConnectTimeout(connectTimeout, TimeUnit.SECONDS);
		mClient.setReadTimeout(readTimeout, TimeUnit.SECONDS);
		mClient.setWriteTimeout(writeTimeout, TimeUnit.SECONDS);

		mHttpCallMap = new ConcurrentHashMap<String, Call>();
	}

	/**
	 * 终止HTTP请求
	 * 
	 * @param url HTTP URL
	 */
	public void stopHttpRequest(String url) {
		Call httpCall = mHttpCallMap.remove(url);
		if (null != httpCall)
			httpCall.cancel();
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// GET
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * HTTP GET 异步获取数据
	 * 
	 * @param url 网络地址
	 * @param headerValues 表头信息
	 * @param httpCallback 回调
	 */
	public void asyncGetDataByHttp(String url, HashMap<String, String> headerValues,
			HttpResponseCallback responseCallback) {

		this.asyncGetDataByHttp(url, headerValues, 0, responseCallback);
	}

	/**
	 * HTTP GET 异步获取数据
	 * 
	 * @param url 网络地址
	 * @param headerValues 表头信息
	 * @param cacheSize 响应数据缓存区大小，单位：字节
	 * @param responseCallback 回调
	 */
	public void asyncGetDataByHttp(String url, HashMap<String, String> headerValues,
			int readCacheSize, HttpResponseCallback responseCallback) {

		Request request = httpGetRequest(url, headerValues);
		Call call = mClient.newCall(request);
		mHttpCallMap.put(url, call);
		call.enqueue(new ResponseCallback(url, readCacheSize, responseCallback));
	}

	// 创建HTTP请求对象
	private Request httpGetRequest(String url, HashMap<String, String> headerValues) {
		Builder requestBuilder = new Request.Builder().url(url).header("User-Agent",
				USER_AGENT);

		// 设置表头
		if (null != headerValues && headerValues.size() > 0) {
			Iterator<Entry<String, String>> headerIterator = headerValues.entrySet()
					.iterator();
			Entry<String, String> headerEntry = null;

			while (null != (headerEntry = headerIterator.next())) {
				String headerKey = headerEntry.getKey();
				String headerValue = headerEntry.getValue();
				requestBuilder = requestBuilder.addHeader(headerKey, headerValue);
			}
		}

		return requestBuilder.build();
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Post
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 异步上传文字数据
	 * @param url 网络地址
	 * @param dataType 要上传的数据的类型
	 * @param postString 要上传的字符串
	 * @param readCacheSize 数据读取缓存缓存区长度，单位字节
	 * @param responseCallback 回调
	 */
	public void syncPostStringByHttp(String url, MediaType dataType, String postString, int readCacheSize,
			HttpPostResponseCallback responseCallback) {
		
		RequestBody requestBody = RequestBody.create(dataType, postString);
		this.syncPostDataByHttp(url, requestBody, readCacheSize, responseCallback);
	}
	
	/**
	 * 异步上传文件
	 * 
	 * @param url 网络地址
	 * @param dataType 要上传的数据的类型
	 * @param file 文件对象
	 * @param readCacheSize 数据读取缓存缓存区长度，单位字节
	 * @param responseCallback 回调
	 */
	public void syncPostFileByHttp(String url, MediaType dataType, File file, int readCacheSize,
			HttpPostResponseCallback responseCallback) {
		
		RequestBody requestBody = RequestBody.create(dataType, file);
		this.syncPostDataByHttp(url, requestBody, readCacheSize, responseCallback);
	}
	
	/**
	 * HTTP POST 异步上传字节类型数据，该方法在回调中返回POST进度
	 * 
	 * @param url 网络地址
	 * @param dataType 要上传的数据的类型
	 * @param data 要上传的数据
	 * @param readCacheSize 数据读取缓存缓存区长度，单位字节
	 * @param responseCallback 回调
	 */
	public void syncPostBytesByHttp(String url, MediaType dataType, byte[] data, int readCacheSize,
			HttpPostResponseCallback responseCallback) {

		RequestBody requestBody = new PostRequestBody(url, dataType, data, responseCallback);
		this.syncPostDataByHttp(url, requestBody, readCacheSize, responseCallback);
	}

	/**
	 * HTTP POST 异步上传数据
	 * 
	 * @param url 网络地址
	 * @param requestBody POST数据体对象
	 * @param cacheSize 缓存
	 * @param postCallback 回调
	 */
	public void syncPostDataByHttp(String url, RequestBody requestBody,
			int readCacheSize, HttpPostResponseCallback responseCallback) {

		Request request = httpPostRequest(url, requestBody);
		Call call = mClient.newCall(request);
		mHttpCallMap.put(url, call);
		call.enqueue(new ResponseCallback(url, readCacheSize, responseCallback));
	}

	private Request httpPostRequest(String url, RequestBody requestBody) {
		Builder requestBuilder = new Request.Builder().url(url).header("User-Agent",
				USER_AGENT);
		requestBuilder = requestBuilder.url(url).post(requestBody);

		return requestBuilder.build();
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 回调事件
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private class PostRequestBody extends RequestBody {
		private MediaType mediaType = null;
		private String url = null;
		private byte[] data = null;
		private HttpPostResponseCallback postCallback = null;

		private int writeLen = 8 * 1024;

		public PostRequestBody(String url, MediaType mediaType, byte[] data,
				HttpPostResponseCallback postCallback) {

			this.mediaType = mediaType;
			this.url = url;
			this.data = data;
			this.postCallback = postCallback;
		}

		@Override
		public void writeTo(BufferedSink sink) {
			OutputStream out = sink.outputStream();

			try {
				int offset = 0;
				int count = data.length > writeLen ? data.length : writeLen / 2;
				int len = 0;

				while (offset < data.length) {
					out.write(data, offset, count);
					len = count;

					offset += count;
					if (offset + count > data.length)
						count = data.length - offset;

					if (offset >= data.length || offset % writeLen == 0) {
						out.flush();
						postCallback.postData(url, len);
					}
				}
			}
			catch (IOException e) {
				stopHttpRequest(url);
			}
		}

		@Override
		public MediaType contentType() {
			return mediaType;
		}
	}

	// HTTP连接响应回调
	private class ResponseCallback implements Callback {
		private String url = null;
		private HttpResponseCallback callback;
		private int cacheSize = 2 * 1024;

		public ResponseCallback(String url, int cacheSize, HttpResponseCallback callback) {
			this.url = url;
			this.callback = callback;
			
			if(cacheSize > 0)
				this.cacheSize = cacheSize;
		}

		@Override
		public void onFailure(Request request, IOException ex) {
			callback.requestFail(url, ERR_TYPE_IO, 0, ex.getMessage());
		}

		@Override
		public void onResponse(Response response) {
			try {
				if (response.isSuccessful()) {
					InputStream inputStream = response.body().byteStream();
					byte[] dataCache = new byte[cacheSize];
					int len = 0;

					String dataLengthStr = response.header("Content-Length");
					Pattern pattern = Pattern.compile("[0-9]*");
					long dataLength = 0L;
					if (!TextUtils.isEmpty(dataLengthStr)
							&& pattern.matcher(dataLengthStr).matches()) {
						dataLength = Long.parseLong(dataLengthStr);
					}

					callback.requestSuccess(url, response.code(), dataLength);

					while ((len = inputStream.read(dataCache)) > 0) {
						callback.responseData(url, dataCache, len);
					}

					inputStream.close();
					callback.responseSuccess(url);
				}
				else {
					callback.requestFail(url, ERR_TYPE_NET, response.code(),
							response.message());
				}
			}
			catch (IOException ex) {
				callback.requestFail(url, ERR_TYPE_IO, response.code(), ex.getMessage());
			}
			finally {
				mHttpCallMap.remove(url);
			}
		}
	}
}