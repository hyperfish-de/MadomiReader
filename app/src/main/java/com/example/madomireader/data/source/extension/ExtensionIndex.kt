package com.example.madomireader.data.source.extension

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Một extension trong index của repo (ví dụ repo Keiyoushi).
 *
 * Một extension APK có thể cung cấp nhiều nguồn truyện, nên [sources] là danh sách.
 */
data class ExtensionIndexEntry(
    val name: String,
    val packageName: String,
    val apkUrl: String,
    val iconUrl: String?,
    val extensionLib: String?,
    val versionCode: Int,
    val versionName: String,
    val contentWarning: String?,
    val sources: List<ExtensionIndexSource>,
) {
    /** Extension có nội dung người lớn hay không (repo ghi trong `contentWarning`). */
    val isNsfw: Boolean
        get() = contentWarning?.contains("NSFW", ignoreCase = true) == true

    /** Số nguồn mà extension này cung cấp. */
    val sourceCount: Int get() = sources.size
}

/** Một nguồn nằm trong một extension của index. */
data class ExtensionIndexSource(
    val id: String,
    val name: String,
    val language: String,
    val homeUrl: String?,
)

/**
 * Đọc index của repo extension.
 *
 * Repo Keiyoushi công bố index dưới hai dạng: `index.json` và `index.pb` (protobuf nén).
 * Ứng dụng chỉ dùng bản JSON cho gọn — xem ghi chú `shortcut:` trong
 * [ExtensionManager.normalizeRepoUrl].
 */
internal object ExtensionIndexParser {

    fun parse(gson: Gson, json: String): List<ExtensionIndexEntry> {
        val dto = gson.fromJson(json, ExtensionIndexDto::class.java)
            ?: return emptyList()
        return dto.extensionList?.extensions.orEmpty().mapNotNull { it.toEntry() }
    }

    private fun ExtensionDto.toEntry(): ExtensionIndexEntry? {
        val pkg = packageName?.takeIf { it.isNotBlank() } ?: return null
        val apk = resources?.apkUrl?.takeIf { it.isNotBlank() } ?: return null
        return ExtensionIndexEntry(
            name = name?.takeIf { it.isNotBlank() } ?: pkg,
            packageName = pkg,
            apkUrl = apk,
            iconUrl = resources?.iconUrl,
            extensionLib = extensionLib,
            versionCode = versionCode?.toIntOrNull() ?: 0,
            versionName = versionName.orEmpty(),
            contentWarning = contentWarning,
            sources = sources.map { source ->
                ExtensionIndexSource(
                    id = source.id.orEmpty(),
                    name = source.name.orEmpty(),
                    language = source.language.orEmpty(),
                    homeUrl = source.homeUrl,
                )
            },
        )
    }
}

// --------------------------------------------------------------------- DTO thô

internal data class ExtensionIndexDto(
    @SerializedName("extensionList") val extensionList: ExtensionListDto? = null,
)

internal data class ExtensionListDto(
    @SerializedName("extensions") val extensions: List<ExtensionDto> = emptyList(),
)

internal data class ExtensionDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("packageName") val packageName: String? = null,
    @SerializedName("resources") val resources: ExtensionResourcesDto? = null,
    @SerializedName("extensionLib") val extensionLib: String? = null,
    // Trong index JSON, versionCode là chuỗi (ví dụ "106005") nhưng Gson đọc được cả số.
    @SerializedName("versionCode") val versionCode: String? = null,
    @SerializedName("versionName") val versionName: String? = null,
    @SerializedName("contentWarning") val contentWarning: String? = null,
    @SerializedName("sources") val sources: List<ExtensionSourceDto> = emptyList(),
)

internal data class ExtensionResourcesDto(
    @SerializedName("apkUrl") val apkUrl: String? = null,
    @SerializedName("iconUrl") val iconUrl: String? = null,
    @SerializedName("jarUrl") val jarUrl: String? = null,
)

internal data class ExtensionSourceDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("homeUrl") val homeUrl: String? = null,
)
