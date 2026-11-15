package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.HostProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Thêm `User-Agent` mặc định cho mọi request chưa có header này.
 *
 * `KeiSource` kiểm tra interceptor này có mặt trong client mặc định bằng tên lớp.
 */
class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("User-Agent") != null) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header("User-Agent", HostProvider.userAgent)
                .build(),
        )
    }
}
