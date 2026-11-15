package com.example.madomireader.di

import android.content.Context
import com.example.madomireader.data.local.HistoryRepository
import com.example.madomireader.data.local.SettingsRepository
import com.example.madomireader.data.remote.PageBitmapLoader
import com.example.madomireader.data.remote.gemini.GeminiTranslator
import com.example.madomireader.data.repository.MangaRepository
import com.example.madomireader.data.source.SourceRegistry
import com.example.madomireader.data.source.mock.OfflineDemoSource
import com.example.madomireader.data.source.mock.OnlineDemoSource
import com.example.madomireader.domain.ocr.OcrEngine
import com.example.madomireader.domain.translate.TranslationService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Bộ chứa phụ thuộc đơn giản (manual DI) — đủ dùng cho ứng dụng một module,
 * không cần Hilt/Dagger.
 *
 * Muốn thêm nguồn truyện mới: thêm một dòng vào [SourceRegistry] bên dưới.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()

    val okHttpClient: OkHttpClient = GeminiTranslator.defaultClient()
        .newBuilder()
        .cache(Cache(File(appContext.cacheDir, "http_cache"), HTTP_CACHE_BYTES))
        .build()

    val settingsRepository: SettingsRepository = SettingsRepository(appContext)

    val historyRepository: HistoryRepository = HistoryRepository(appContext, gson)

    val sourceRegistry: SourceRegistry = SourceRegistry(
        listOf(
            OfflineDemoSource(),
            OnlineDemoSource(),
        ),
    )

    val mangaRepository: MangaRepository = MangaRepository(sourceRegistry)

    val geminiTranslator: GeminiTranslator = GeminiTranslator(okHttpClient, gson)

    val translationService: TranslationService = TranslationService(geminiTranslator, settingsRepository)

    val ocrEngine: OcrEngine = OcrEngine()

    val pageBitmapLoader: PageBitmapLoader = PageBitmapLoader(okHttpClient)

    private companion object {
        const val HTTP_CACHE_BYTES = 64L * 1024L * 1024L
    }
}
