package com.example.madomireader.data.remote

import android.graphics.Bitmap
import android.util.LruCache
import com.example.madomireader.core.util.BitmapUtils
import com.example.madomireader.domain.model.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Lấy bitmap của một trang truyện để đưa vào OCR.
 *
 * - Trang của nguồn offline được vẽ tại chỗ.
 * - Trang của nguồn online được tải bằng OkHttp (dùng chung cache HTTP với client),
 *   giải mã có tiết chế bộ nhớ và giữ trong LruCache để lần dịch sau không phải tải lại.
 */
class PageBitmapLoader(
    private val client: OkHttpClient,
) {

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHED_PAGES) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    suspend fun load(page: Page, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap =
        withContext(Dispatchers.IO) {
            val key = cacheKey(page)
            cache.get(key)?.takeIf { !it.isRecycled }?.let { return@withContext it }

            val bitmap = if (page.isLocal) {
                PlaceholderPageRenderer.render(page)
            } else {
                download(page, maxDimension)
            }

            cache.put(key, bitmap)
            bitmap
        }

    /** Bitmap đã có sẵn trong cache (không tải mạng). */
    fun cached(page: Page): Bitmap? = cache.get(cacheKey(page))?.takeIf { !it.isRecycled }

    fun invalidate(page: Page) {
        cache.remove(cacheKey(page))
    }

    fun clear() {
        cache.evictAll()
    }

    private fun download(page: Page, maxDimension: Int): Bitmap {
        if (page.imageUrl.isBlank()) throw IOException("Trang ${page.index + 1} không có ảnh")

        val request = Request.Builder()
            .url(page.imageUrl)
            .apply { page.headers.forEach { (name, value) -> addHeader(name, value) } }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Tải trang thất bại: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: throw IOException("Trang trả về rỗng")
            return BitmapUtils.decodeScaled(bytes, maxDimension)
        }
    }

    private fun cacheKey(page: Page): String = when {
        page.isLocal -> "local:${page.index}:${page.placeholderLines.hashCode()}"
        else -> page.imageUrl
    }

    companion object {
        private const val MAX_CACHED_PAGES = 10
        private const val DEFAULT_MAX_DIMENSION = 1600
    }
}
