package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Bọc mọi lỗi không lường trước thành [IOException] để OkHttp xử lý như lỗi mạng
 * thay vì làm sập luồng gọi.
 *
 * `KeiSource` kiểm tra interceptor này có mặt trong client mặc định bằng tên lớp.
 */
class UncaughtExceptionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = try {
        chain.proceed(chain.request())
    } catch (error: Throwable) {
        if (error is Error) throw error
        throw IOException(error.message ?: "Lỗi không xác định khi gọi mạng", error)
    }
}
