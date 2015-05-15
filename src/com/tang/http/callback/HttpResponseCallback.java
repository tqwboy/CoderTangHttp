package com.tang.http.callback;

public interface HttpResponseCallback {
	/**
	 * HTTP Request成功回调
	 * 
	 * @param url HTTP URL
	 * @param httpCode HTTP Code
	 * @param dataLength 数据长度
	 */
	public void requestSuccess(String url, int httpCode, long dataLength);
	
	/**
	 * HTTP数据获取回调
	 * 
	 * @param url HTTP URL
	 * @param data 服务器返回的数据
	 * @param len 数据长度
	 */
	public void responseData(String url, byte[] data, int len);
	
	/**
	 * HTTP成功结束
	 * 
	 * @param url HTTP URL
	 */
	public void responseSuccess(String url);
	
	/**
	 * HTTP Request失败回调
	 * 
	 * @param url HTTP URL
	 * @param errType 错误类型
	 * @param httpCode HTTP Code
	 * @param errMsg 错误信息
	 */
	public void requestFail(String url, int errType, int httpCode, String errMsg);
}