package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Chỗ dành cho việc vượt Cloudflare (Mihon dùng WebView để lấy cookie `cf_clearance`).
 *
 * Ở đây nó chỉ là một bước chuyển tiếp: các extension bắt buộc phải thấy một interceptor
 * tên `CloudflareInterceptor` trong client mặc định, nhưng ứng dụng này không mở WebView
 * để giải Cloudflare.
 */
class CloudflareInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
