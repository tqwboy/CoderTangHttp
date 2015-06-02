package com.tang.http;

import com.squareup.okhttp.MediaType;

public class PostMediaType {
	/**
	 * 二进制流，不知道文件类型
	 */
	public static final MediaType MEDIA_TYPE_STREAM = MediaType
			.parse("application/octet-stream");

	/** xml，UTF-8编码 */
	public static final MediaType MEDIA_TYPE_XML = MediaType.parse("txt/xml; charset=utf-8");

	/** JSON，UTF-8编码 */
	public static final MediaType MEDIA_TYPE_JSON_UTF8 = MediaType
			.parse("application/json; charset=utf-8");

	/** png格式图片 */
	public static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

	/** jpeg格式图片 */
	public static final MediaType MEDIA_TYPE_JPEG = MediaType
			.parse("image/jpeg");
}