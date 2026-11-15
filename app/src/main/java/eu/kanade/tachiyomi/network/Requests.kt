@file:Suppress("Unused", "FunctionName")

package eu.kanade.tachiyomi.network

import eu.kanade.tachiyomi.HostProvider
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private val DEFAULT_CACHE_CONTROL: CacheControl = CacheControl.Builder().build()

private val DEFAULT_HEADERS: Headers
    get() = Headers.Builder()
        .add("User-Agent", HostProvider.userAgent)
        .build()

private val DEFAULT_BODY: RequestBody = ByteArray(0).toRequestBody(null)

fun GET(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request = Request.Builder()
    .url(url)
    .headers(headers)
    .cacheControl(cache)
    .build()

/**
 * @since tachiyomix 1.4
 */
fun GET(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request = Request.Builder()
    .url(url)
    .headers(headers)
    .cacheControl(cache)
    .build()

fun POST(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request = Request.Builder()
    .url(url)
    .headers(headers)
    .cacheControl(cache)
    .post(body)
    .build()
