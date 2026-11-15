package com.example.madomireader.data.source.extension

import android.content.Context
import android.util.Log
import com.example.madomireader.data.source.MangaSource
import com.example.madomireader.data.source.SourceRegistry
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Quản lý các extension (nguồn truyện ngoài):
 *  - tải index của repo (mặc định là repo Keiyoushi),
 *  - cài / cập nhật / gỡ extension (tải APK về, nạp bằng [ExtensionLoader]),
 *  - đẩy danh sách nguồn đang có vào [SourceRegistry] để giao diện dùng ngay.
 */
class ExtensionManager(
    private val context: Context,
    private val gson: Gson,
    private val client: OkHttpClient,
    private val registry: SourceRegistry,
) {

    private val store = ExtensionStore(context, gson)
    private val loader = ExtensionLoader(context)

    private val _installed = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val installed: StateFlow<List<InstalledExtension>> = _installed.asStateFlow()

    private val _available = MutableStateFlow<List<ExtensionIndexEntry>>(emptyList())
    val available: StateFlow<List<ExtensionIndexEntry>> = _available.asStateFlow()

    private val _busyPackages = MutableStateFlow<Set<String>>(emptySet())
    val busyPackages: StateFlow<Set<String>> = _busyPackages.asStateFlow()

    /** Nguồn đã dựng, khoá theo id nguồn — giữ lại để không phải nạp lại APK mỗi lần. */
    private val sourceCache = ConcurrentHashMap<String, MangaSource>()

    // ------------------------------------------------------------------- khởi động

    /** Đọc danh sách đã cài và nạp lại các nguồn. Gọi một lần lúc ứng dụng khởi động. */
    suspend fun restore() = withContext(Dispatchers.IO) {
        val installed = store.load()
        _installed.value = installed
        rebuildSources(installed)
    }

    // ----------------------------------------------------------------------- index

    /**
     * Tải index của repo và trả về danh sách extension có thể cài.
     *
     * @param repoUrl URL do người dùng nhập; xem [normalizeRepoUrl].
     */
    suspend fun refreshIndex(repoUrl: String): Result<List<ExtensionIndexEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = normalizeRepoUrl(repoUrl)
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Máy chủ trả về HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) error("Index rỗng")

                    val entries = ExtensionIndexParser.parse(gson, body)
                        .sortedBy { it.name.lowercase() }
                    if (entries.isEmpty()) error("Index không có extension nào")
                    _available.value = entries
                    entries
                }
            }
        }

    // ------------------------------------------------------------------- cài / gỡ

    fun isInstalled(packageName: String): Boolean =
        _installed.value.any { it.packageName == packageName }

    fun installedVersion(packageName: String): Int? =
        _installed.value.firstOrNull { it.packageName == packageName }?.versionCode

    /** Có bản mới hơn đang chờ cài hay không. */
    fun hasUpdate(entry: ExtensionIndexEntry): Boolean {
        val current = installedVersion(entry.packageName) ?: return false
        return entry.versionCode > current
    }

    suspend fun install(entry: ExtensionIndexEntry): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            markBusy(entry.packageName, true)
            try {
                val target = File(extensionsDir(context), fileNameFor(entry))
                download(entry.apkUrl, target)

                val meta = loader.readMetaData(target)
                    ?: error("APK không phải extension hợp lệ")
                val factory = loader.createFactory(target, meta.className, entry.packageName)
                    ?: error("Không nạp được extension")
                if (factory.createSources().isEmpty()) {
                    target.delete()
                    error("Extension không cung cấp nguồn truyện nào")
                }

                val previous = _installed.value.firstOrNull { it.packageName == entry.packageName }
                val installedEntry = InstalledExtension(
                    packageName = entry.packageName,
                    name = meta.name ?: entry.name,
                    versionCode = entry.versionCode,
                    versionName = entry.versionName,
                    extensionLib = entry.extensionLib,
                    apkFileName = target.name,
                    iconUrl = entry.iconUrl,
                    isNsfw = entry.isNsfw || meta.isNsfw,
                )

                val next = _installed.value
                    .filterNot { it.packageName == entry.packageName } + installedEntry
                store.save(next)
                _installed.value = next

                if (previous != null && previous.apkFileName != target.name) {
                    runCatching { previous.apkFile(context).delete() }
                }

                rebuildSources(next)
            } finally {
                markBusy(entry.packageName, false)
            }
        }
    }

    suspend fun uninstall(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            markBusy(packageName, true)
            try {
                val removed = _installed.value.firstOrNull { it.packageName == packageName }
                val next = _installed.value.filterNot { it.packageName == packageName }
                store.save(next)
                _installed.value = next

                removed?.let { runCatching { it.apkFile(context).delete() } }
                rebuildSources(next)
            } finally {
                markBusy(packageName, false)
            }
        }
    }

    // ---------------------------------------------------------------------- nội bộ

    private fun rebuildSources(installed: List<InstalledExtension>) {
        val sources = mutableListOf<MangaSource>()
        installed.forEach { extension ->
            val apk = extension.apkFile(context)
            if (!apk.exists()) {
                Log.w(TAG, "Thiếu APK của extension ${extension.packageName}")
                return@forEach
            }

            val built = runCatching { buildSourcesOf(extension, apk) }
                .onFailure { error ->
                    Log.w(TAG, "Không nạp được extension ${extension.packageName}: ${error.message}")
                }
                .getOrDefault(emptyList())

            built.forEach { source -> sourceCache[source.id] = source }
            sources += built
        }

        // Gỡ khỏi cache những nguồn không còn extension tương ứng.
        val aliveIds = sources.map { it.id }.toSet()
        sourceCache.keys.retainAll(aliveIds)

        registry.setAll(sources.sortedBy { it.name.lowercase() })
    }

    private fun buildSourcesOf(extension: InstalledExtension, apk: File): List<MangaSource> {
        val meta = loader.readMetaData(apk) ?: return emptyList()
        val factory = loader.createFactory(apk, meta.className, extension.packageName)
            ?: return emptyList()
        return factory.createSources().map { source ->
            ExtensionMangaSource(source, extension.name)
        }
    }

    private fun download(url: String, target: File) {
        if (url.isBlank()) error("Extension không có link tải APK")
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Tải APK thất bại: HTTP ${response.code}")
            val body = response.body ?: error("APK trả về rỗng")
            target.parentFile?.mkdirs()
            body.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        // Android 14 không cho nạp file dex còn ghi được.
        runCatching { target.setReadOnly() }
    }

    private fun markBusy(packageName: String, busy: Boolean) {
        _busyPackages.value = if (busy) {
            _busyPackages.value + packageName
        } else {
            _busyPackages.value - packageName
        }
    }

    private fun fileNameFor(entry: ExtensionIndexEntry): String =
        "${entry.packageName}-${entry.versionCode}.apk"

    companion object {
        private const val TAG = "ExtensionManager"

        /**
         * Repo extension mặc định (Keiyoushi).
         *
         * Ghi chú `shortcut:` Repo còn công bố bản nén protobuf `index.pb`, nhưng ứng dụng
         * chỉ đọc bản JSON cho gọn; URL `.pb` do người dùng nhập được tự đổi sang `.json`.
         */
        const val DEFAULT_REPO_URL: String =
            com.example.madomireader.domain.model.AppSettings.DEFAULT_EXTENSION_REPO_URL

        /**
         * Chuẩn hoá URL index:
         *  - `.../index.pb` → `.../index.json`
         *  - `.../`         → `.../index.json`
         *  - `.../repo`     → `.../repo/index.json`
         */
        fun normalizeRepoUrl(raw: String): String {
            var url = raw.trim()
            if (url.isEmpty()) return DEFAULT_REPO_URL
            if (url.endsWith(".pb", ignoreCase = true)) {
                url = url.dropLast(3) + ".json"
            }
            if (url.endsWith("/")) {
                url += "index.json"
            } else if (!url.endsWith(".json", ignoreCase = true)) {
                url += "/index.json"
            }
            return url
        }
    }
}
