package com.tang.http.callback;

public interface HttpPostResponseCallback extends HttpResponseCallback {
	/**
	 * POST 上传数据回调
	 * 
	 * @param url HTTP URL
	 * @param len 上传的数据的长度
	 */
	public void postData(String url, int len);
}