package com.example.madomireader.data.source.extension

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dalvik.system.DexClassLoader
import eu.kanade.tachiyomi.source.SourceFactory
import java.io.File
import java.lang.reflect.Modifier

/**
 * Thông tin đọc từ `AndroidManifest.xml` của một APK extension.
 */
data class ExtensionMetaData(
    val className: String,
    val name: String?,
    val isNsfw: Boolean,
)

/**
 * Nạp các APK extension (định dạng của Keiyoushi/Tachiyomi) bằng [DexClassLoader].
 *
 * Không cài APK như một ứng dụng Android — chỉ đọc manifest và nạp lớp vào tiến trình
 * hiện tại. Nhờ vậy extension thấy đúng bộ API `eu.kanade.tachiyomi.*` của ứng dụng
 * (classloader cha là classloader của app, và luôn ưu tiên lớp của cha).
 */
class ExtensionLoader(private val context: Context) {

    /** Đọc meta-data của APK. Trả về null nếu APK không phải extension hợp lệ. */
    @Suppress("DEPRECATION")
    fun readMetaData(apkFile: File): ExtensionMetaData? {
        if (!apkFile.exists()) return null

        val info = context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_META_DATA,
        ) ?: return null

        val meta = info.applicationInfo?.metaData ?: return null
        val className = meta.getString(KEY_CLASS)?.takeIf { it.isNotBlank() } ?: return null
        val name = meta.getString(KEY_NAME)?.takeIf { it.isNotBlank() }
        val isNsfw = meta.getBoolean(KEY_NSFW, false) ||
            meta.getString(KEY_CONTENT_WARNING)?.contains("NSFW", ignoreCase = true) == true

        return ExtensionMetaData(className = className, name = name, isNsfw = isNsfw)
    }

    /**
     * Nạp lớp vào tiến trình và tạo instance của [SourceFactory].
     *
     * @param packageName dùng để đặt tên thư mục tối ưu hoá dex (mỗi extension một thư mục).
     */
    fun createFactory(apkFile: File, className: String, packageName: String): SourceFactory? {
        // Android 14 trở lên từ chối nạp file dex còn ghi được.
        runCatching { apkFile.setReadOnly() }

        val optimizedDir = File(context.codeCacheDir, "ext-${packageName.hashCode().toUInt().toString(16)}")
        optimizedDir.mkdirs()

        val loader = DexClassLoader(
            apkFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            javaClass.classLoader,
        )

        val clazz = Class.forName(className, true, loader)
        val instance = newInstance(clazz) ?: return null
        return instance as? SourceFactory
    }

    private fun newInstance(clazz: Class<*>): Any? {
        // Lớp vào của extension là một `object` Kotlin (có trường INSTANCE).
        runCatching {
            val field = clazz.getField("INSTANCE")
            if (Modifier.isStatic(field.modifiers)) {
                return field.get(null)
            }
        }.onFailure { error ->
            Log.d(TAG, "Không có INSTANCE trên ${clazz.name}: ${error.message}")
        }

        // Dự phòng: hàm khởi tạo không tham số.
        return runCatching { clazz.getDeclaredConstructor().newInstance() }.getOrNull()
    }

    private companion object {
        const val TAG = "ExtensionLoader"

        /** Khoá meta-data chuẩn của Tachiyomi/Mihon: tên lớp vào của extension. */
        const val KEY_CLASS = "tachiyomi.extension.class"

        /** Khoá của Keiyoushi: tên hiển thị của extension. */
        const val KEY_NAME = "tachiyomix.name"

        /** Khoá chuẩn: extension có nội dung 18+. */
        const val KEY_NSFW = "tachiyomi.extension.nsfw"

        /** Khoá của Keiyoushi: cảnh báo nội dung (ví dụ CONTENT_WARNING_NSFW). */
        const val KEY_CONTENT_WARNING = "tachiyomix.contentWarning"
    }
}
