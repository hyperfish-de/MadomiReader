package com.example.madomireader.di

import android.app.Application
import android.content.Context
import com.example.madomireader.data.local.HistoryRepository
import com.example.madomireader.data.local.SettingsRepository
import com.example.madomireader.data.remote.PageBitmapLoader
import com.example.madomireader.data.remote.gemini.GeminiTranslator
import com.example.madomireader.data.remote.google.GoogleTranslator
import com.example.madomireader.data.remote.mlkit.MlKitTranslator
import com.example.madomireader.data.repository.MangaRepository
import com.example.madomireader.data.source.SourceRegistry
import com.example.madomireader.data.source.extension.ExtensionManager
import com.example.madomireader.data.source.extension.InjektHost
import com.example.madomireader.domain.ocr.OcrEngine
import com.example.madomireader.domain.translate.TranslationService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.HostProvider
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Bộ chứa phụ thuộc đơn giản (manual DI) — đủ dùng cho ứng dụng một module,
 * không cần Hilt/Dagger.
 *
 * Nguồn truyện không còn được viết cứng ở đây: [SourceRegistry] bắt đầu rỗng và được
 * [ExtensionManager] đổ đầy khi người dùng cài extension.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Vòng đời sống bằng cả ứng dụng — dùng cho các việc nền như nạp lại extension. */
    private val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()

    /**
     * OkHttp client dùng chung cho cả ứng dụng và các extension.
     *
     * Ba interceptor dưới đây bắt buộc phải có mặt (các extension của Keiyoushi kiểm tra
     * chúng theo tên lớp lúc chạy).
     */
    val okHttpClient: OkHttpClient = GeminiTranslator.defaultClient()
        .newBuilder()
        .cache(Cache(File(appContext.cacheDir, "http_cache"), HTTP_CACHE_BYTES))
        .addInterceptor(UncaughtExceptionInterceptor())
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(CloudflareInterceptor())
        .build()

    init {
        // Bộ API host (`eu.kanade.tachiyomi.*`) phải sẵn sàng trước khi nạp extension đầu tiên:
        // `HttpSource` dựng `NetworkHelper(HostProvider.context)` ngay trong hàm khởi tạo.
        AppInfo.install(APP_VERSION_CODE, APP_VERSION_NAME)
        HostProvider.install(appContext, okHttpClient)
        (appContext as? Application)?.let { application -> InjektHost.install(application) }
    }

    val settingsRepository: SettingsRepository = SettingsRepository(appContext)

    val historyRepository: HistoryRepository = HistoryRepository(appContext, gson)

    val sourceRegistry: SourceRegistry = SourceRegistry()

    val mangaRepository: MangaRepository = MangaRepository(sourceRegistry)

    val extensionManager: ExtensionManager =
        ExtensionManager(appContext, gson, okHttpClient, sourceRegistry)

    val geminiTranslator: GeminiTranslator = GeminiTranslator(okHttpClient, gson)

    val googleTranslator: GoogleTranslator = GoogleTranslator(okHttpClient)

    val mlKitTranslator: MlKitTranslator = MlKitTranslator()

    val translationService: TranslationService = TranslationService(
        geminiTranslator = geminiTranslator,
        googleTranslator = googleTranslator,
        mlKitTranslator = mlKitTranslator,
        settingsRepository = settingsRepository,
    )

    val ocrEngine: OcrEngine = OcrEngine()

    val pageBitmapLoader: PageBitmapLoader = PageBitmapLoader(okHttpClient)

    init {
        // Nạp lại các extension đã cài (đọc APK từ bộ nhớ riêng rồi đẩy nguồn vào registry).
        applicationScope.launch { extensionManager.restore() }
    }

    private companion object {
        const val HTTP_CACHE_BYTES = 64L * 1024L * 1024L

        /** Khớp `versionCode`/`versionName` trong `app/build.gradle.kts`. */
        const val APP_VERSION_CODE = 1
        const val APP_VERSION_NAME = "1.0.0"
    }
}
