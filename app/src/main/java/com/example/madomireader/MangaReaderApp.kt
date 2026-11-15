package com.example.madomireader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

        // Khi người dùng cài/gỡ/cập nhật một extension (app Android), quét lại để
        // Madomi Reader nhận diện extension mới mà không cần khởi động lại.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    container.rescanExtensions()
                }
            },
            filter,
        )
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
