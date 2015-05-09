package com.tang.http;

import com.squareup.okhttp.MediaType;

public class PostMediaType {
	/**
	 * UTF-8编码的JSON
	 */
	public static final MediaType MEDIA_TYPE_JSON_UTF8 = MediaType
			.parse("application/json; charset=utf-8");

	/**
	 * png格式图片
	 */
	public static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

	/**
	 * png格式图片
	 */
	public static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
}