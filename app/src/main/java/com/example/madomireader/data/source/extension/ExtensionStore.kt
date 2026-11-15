package com.example.madomireader.data.source.extension

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Một extension đã cài trên máy.
 *
 * Không lưu danh sách nguồn ở đây: mỗi lần khởi động, ứng dụng nạp lại APK và hỏi
 * extension xem nó cung cấp nguồn nào ([ExtensionLoader.createFactory]).
 */
data class InstalledExtension(
    val packageName: String,
    val name: String,
    val versionCode: Int,
    val versionName: String,
    val extensionLib: String?,
    val apkFileName: String,
    val iconUrl: String?,
    val isNsfw: Boolean,
) {
    val displayVersion: String
        get() = versionName.ifBlank { versionCode.toString() }
}

/** Thư mục chứa APK của các extension đã cài (trong bộ nhớ riêng của ứng dụng). */
fun extensionsDir(context: Context): File =
    File(context.filesDir, "extensions").apply { mkdirs() }

/** APK của một extension đã cài. */
fun InstalledExtension.apkFile(context: Context): File =
    File(extensionsDir(context), apkFileName)

/**
 * Lưu danh sách extension đã cài vào một file JSON trong `filesDir`.
 *
 * Không cần Room: danh sách này rất nhỏ và chỉ được đọc một lần lúc khởi động.
 */
class ExtensionStore(
    private val context: Context,
    private val gson: Gson,
) {

    private val listType = object : TypeToken<List<InstalledExtension>>() {}.type

    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun load(): List<InstalledExtension> = runCatching {
        if (!file.exists()) return@runCatching emptyList()
        val json = file.readText()
        if (json.isBlank()) emptyList()
        else gson.fromJson<List<InstalledExtension>>(json, listType) ?: emptyList()
    }.getOrDefault(emptyList())

    fun save(extensions: List<InstalledExtension>) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(extensions, listType))
        }
    }

    private companion object {
        const val FILE_NAME = "installed_extensions.json"
    }
}
