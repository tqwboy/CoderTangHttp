package com.tang.http.callback;

public interface HttpResponseCallback {
	public void response(String url, int httpCode, long dataLength);
	
	public void responseData(String url, byte[] data, int len);
	
	public void responseSuccess(String url);
	
	public void requestFail(String url, int errType, int httpCode, String errMsg);
}