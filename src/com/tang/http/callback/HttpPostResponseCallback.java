package com.tang.http.callback;

public interface HttpPostResponseCallback extends HttpResponseCallback {
	public void postData(String url, int len);
}