package com.example.madomireader

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.madomireader.di.AppContainer

/**
 * Lớp Application: tạo [AppContainer] và cấu hình Coil dùng chung OkHttp client
 * (nhờ vậy ảnh truyện được cache đĩa 256 MB và vẫn hoạt động khi mất mạng).
 */
class MangaReaderApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { container.okHttpClient }
        .crossfade(true)
        .respectCacheHeaders(false)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(IMAGE_CACHE_BYTES)
                .build()
        }
        .build()

    private companion object {
        const val IMAGE_CACHE_BYTES = 256L * 1024L * 1024L
    }
}
