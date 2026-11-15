package eu.kanade.tachiyomi.network

import okhttp3.Response

/**
 * Exception cho các mã HTTP mà OkHttp coi là không thành công.
 *
 * @see Response.isSuccessful
 * @since tachiyomix 1.6
 */
class HttpException(val code: Int) : IllegalStateException("HTTP error $code")
