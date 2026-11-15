package eu.kanade.tachiyomi

import android.content.Context
import okhttp3.OkHttpClient

/**
 * Cầu nối giữa ứng dụng và bộ API host.
 *
 * Bộ API `eu.kanade.tachiyomi.*` không có hàm khởi tạo công khai (trong Mihon nó lấy
 * phụ thuộc qua DI nội bộ), nên ở đây ứng dụng "cắm" sẵn OkHttp client và Context dùng chung
 * trước khi nạp bất kỳ extension nào.
 */
object HostProvider {

    @Volatile
    private var httpClient: OkHttpClient? = null

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context, client: OkHttpClient) {
        appContext = context.applicationContext
        httpClient = client
    }

    val context: Context
        get() = appContext ?: error("HostProvider chưa được khởi tạo")

    val client: OkHttpClient
        get() = httpClient ?: error("HostProvider chưa được khởi tạo")

    /** User-Agent mặc định cho các request của extension. */
    val userAgent: String
        get() = "MadomiReader/${AppInfo.getVersionName()}"
}
