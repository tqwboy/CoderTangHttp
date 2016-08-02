package com.tang.http.library;

import android.text.TextUtils;

import com.tang.http.library.callback.HttpPostResponseCallback;
import com.tang.http.library.callback.HttpResponseCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;


public class HttpManager {
	public static final int ERR_TYPE_IO = 0x0001;
	public static final int ERR_TYPE_NET = 0x0002;

	private static final String USER_AGENT = "Mozilla/5.0 Android "
			+ android.os.Build.VERSION.SDK_INT + " CoderTangHttp.jar";

	private OkHttpClient mClient = null;
	private ConcurrentHashMap<String, Call> mHttpCallMap = null;

	public HttpManager(int connectTimeout, int readTimeout, int writeTimeout) {
		OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
		httpBuilder.connectTimeout(connectTimeout, TimeUnit.SECONDS);
		httpBuilder.readTimeout(readTimeout, TimeUnit.SECONDS);
		httpBuilder.writeTimeout(writeTimeout, TimeUnit.SECONDS);
		mClient = httpBuilder.build();

		mHttpCallMap = new ConcurrentHashMap<>();
	}

	/**
	 * 终止HTTP请求
	 * 
	 * @param requestId 请求
	 */
	public void stopHttpRequest(String requestId) {
		Call httpCall = mHttpCallMap.remove(requestId);
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
	 * @param responseCallback 回调
	 *
	 * @return 请求编码，可用来停止请求
	 */
	public String asyncGetDataByHttp(String url, HashMap<String, String> headerValues,
			HttpResponseCallback responseCallback) {

		return asyncGetDataByHttp(url, headerValues, 0, responseCallback);
	}

	/**
	 * HTTP GET 异步获取数据
	 * 
	 * @param url 网络地址
	 * @param headerValues 表头信息
	 * @param readCacheSize 响应数据缓存区大小，单位：字节
	 * @param responseCallback 回调
	 *
	 * @return 请求编码，可用来停止请求
	 */
	public String asyncGetDataByHttp(String url, HashMap<String, String> headerValues,
			int readCacheSize, HttpResponseCallback responseCallback) {

		Request request = httpGetRequest(url, headerValues);
		Call call = mClient.newCall(request);
		mHttpCallMap.put(url, call);
		call.enqueue(new ResponseCallback(url, readCacheSize, responseCallback));

		return url;
	}

	// 创建HTTP请求对象
	private Request httpGetRequest(String url, HashMap<String, String> headerValues) {
		Builder requestBuilder = new Builder().url(url).header("User-Agent",
				USER_AGENT);

		// 设置表头
		if (null != headerValues && headerValues.size() > 0) {
			Iterator<Entry<String, String>> headerIterator = headerValues.entrySet()
					.iterator();

			while (headerIterator.hasNext()) {
				Entry<String, String> headerEntry = headerIterator.next();
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
	 *
	 * @return 请求编号，用来停止请求
	 */
	public String asyncPostStringByHttp(String url, MediaType dataType, String postString, int readCacheSize,
										HttpPostResponseCallback responseCallback) {
		
		RequestBody requestBody = RequestBody.create(dataType, postString);
		return asyncPostDataByHttp(url, requestBody, readCacheSize, responseCallback);
	}
	
	/**
	 * 异步上传文件
	 * 
	 * @param url 网络地址
	 * @param dataType 要上传的数据的类型
	 * @param file 文件对象
	 * @param readCacheSize 数据读取缓存缓存区长度，单位字节
	 * @param responseCallback 回调
	 *
	 * @return 请求编号，用来停止请求
	 */
	public String asyncPostFileByHttp(String url, MediaType dataType, File file, int readCacheSize,
			HttpPostResponseCallback responseCallback) {
		
		RequestBody requestBody = RequestBody.create(dataType, file);
		return asyncPostDataByHttp(url, requestBody, readCacheSize, responseCallback);
	}
	
	/**
	 * HTTP POST 异步上传字节类型数据，该方法在回调中返回POST进度
	 * 
	 * @param url 网络地址
	 * @param dataType 要上传的数据的类型
	 * @param data 要上传的数据
	 * @param readCacheSize 数据读取缓存缓存区长度，单位字节
	 * @param responseCallback 回调
	 *
	 * @return 请求编号，用来停止请求
	 */
	public String asyncPostBytesByHttp(String url, MediaType dataType, byte[] data, int
			readCacheSize,
			HttpPostResponseCallback responseCallback) {

		RequestBody requestBody = new PostRequestBody(url, dataType, data, responseCallback);
		return asyncPostDataByHttp(url, requestBody, readCacheSize, responseCallback);
	}

	/**
	 * HTTP POST 异步上传数据
	 * 
	 * @param url 网络地址
	 * @param requestBody POST数据体对象
	 * @param readCacheSize 缓存大小
	 * @param responseCallback 回调
	 *
	 * @return 请求编号，用来停止请求
	 */
	public String asyncPostDataByHttp(String url, RequestBody requestBody,
			int readCacheSize, HttpPostResponseCallback responseCallback) {

		Request request = httpPostRequest(url, requestBody);
		Call call = mClient.newCall(request);

		UUID uuid = UUID.randomUUID();
		String requestCode = uuid.toString();
		mHttpCallMap.put(requestCode, call);
		call.enqueue(new ResponseCallback(url, readCacheSize, responseCallback));

		return requestCode;
	}

	private Request httpPostRequest(String url, RequestBody requestBody) {
		Builder requestBuilder = new Builder().url(url).header("User-Agent",
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

		private int writeLen = 2 * 1024;

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
				int count = data.length < writeLen ? data.length : writeLen;
				int len;

				while (offset < data.length) {
					out.write(data, offset, count);
					len = count;

					postCallback.postData(url, len);
					out.flush();
					
					offset += count;
					if (offset+count > data.length)
						count = data.length - offset;
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
		public void onFailure(Call call, IOException e) {
			mHttpCallMap.remove(url);
			callback.requestFail(url, ERR_TYPE_IO, 0, e.getMessage());
		}

		@Override
		public void onResponse(Call call, Response response) {
			try {
				if (response.isSuccessful()) {
					InputStream inputStream = response.body().byteStream();
					byte[] dataCache = new byte[cacheSize];
					int len;

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