package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.HostProvider
import okhttp3.OkHttpClient

/**
 * Dịch vụ mạng mà các extension dùng để lấy OkHttp client.
 *
 * Bản gốc trong `extensions-lib` chỉ là stub; ở đây nó trả về client dùng chung của ứng dụng
 * (đã có cache HTTP 64 MB).
 */
@Suppress("UNUSED_PARAMETER")
class NetworkHelper(context: Context) {

    val client: OkHttpClient = HostProvider.client
}
