package com.example.madomireader.data.source.extension

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.madomireader.data.source.MangaSource
import com.example.madomireader.data.source.SourceRegistry
import dalvik.system.DexClassLoader
import eu.kanade.tachiyomi.source.SourceFactory
import java.io.File

/**
 * Quét các ứng dụng extension đã cài trên máy (kiểu Tachiyomi/Mihon):
 * mọi APK có meta-data `tachiyomi.extension.class` đều được xem là extension.
 *
 * Không cần cài APK vào bộ nhớ riêng của ứng dụng: extension đã là một app Android,
 * chúng ta chỉ nạp dex của nó (qua [DexClassLoader]) để lấy các nguồn truyện.
 */
class InstalledExtensionScanner(private val context: Context) {

    /**
     * Quét toàn bộ app đã cài và trả về danh sách nguồn truyện tìm được.
     * Chạy trên luồng nền (IO).
     */
    fun scan(): List<MangaSource> {
        val pm = context.packageManager
        val installed = runCatching {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }.getOrDefault(emptyList())

        val sources = mutableListOf<MangaSource>()
        for (pkg in installed) {
            val appInfo = pkg.applicationInfo ?: continue
            val meta = appInfo.metaData ?: continue
            val className = meta.getString(KEY_CLASS)?.takeIf { it.isNotBlank() } ?: continue

            val apkPath = appInfo.sourceDir ?: continue
            val name = meta.getString(KEY_NAME)?.takeIf { it.isNotBlank() }
                ?: runCatching { appInfo.loadLabel(pm).toString() }.getOrNull()
                ?: pkg.packageName

            val loaded = runCatching { loadSources(apkPath, className, pkg.packageName, name) }
                .onFailure { error ->
                    Log.w(TAG, "Không nạp được extension ${pkg.packageName}: ${error.message}")
                }
                .getOrDefault(emptyList())

            sources += loaded
        }
        return sources.sortedBy { it.name.lowercase() }
    }

    private fun loadSources(
        apkPath: String,
        className: String,
        packageName: String,
        name: String,
    ): List<MangaSource> {
        val optimizedDir = File(context.codeCacheDir, "ext-${packageName.hashCode().toUInt().toString(16)}")
        optimizedDir.mkdirs()

        val loader = DexClassLoader(apkPath, optimizedDir.absolutePath, null, context.classLoader)
        val clazz = Class.forName(className, true, loader)

        val factory = newInstance(clazz) as? SourceFactory ?: return emptyList()
        return factory.createSources().map { source ->
            ExtensionMangaSource(source, name)
        }
    }

    private fun newInstance(clazz: Class<*>): Any? {
        runCatching {
            val field = clazz.getField("INSTANCE")
            if (java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                return field.get(null)
            }
        }
        return runCatching { clazz.getDeclaredConstructor().newInstance() }.getOrNull()
    }

    companion object {
        private const val TAG = "InstalledExtensionScanner"

        private const val KEY_CLASS = "tachiyomi.extension.class"
        private const val KEY_NAME = "tachiyomix.name"
    }
}
