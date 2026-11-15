package com.example.madomireader.domain.model

/**
 * Trạng thái phát hành của một bộ truyện.
 */
enum class MangaStatus(val label: String) {
    ONGOING("Đang tiến hành"),
    COMPLETED("Hoàn thành"),
    PAUSED("Tạm dừng"),
    CANCELLED("Đã hủy"),
    UNKNOWN("Không rõ");

    companion object {
        fun fromRaw(raw: String?): MangaStatus {
            val value = raw?.trim()?.lowercase() ?: return UNKNOWN
            return when {
                value.contains("ongoing") || value.contains("đang") || value.contains("publishing") -> ONGOING
                value.contains("completed") || value.contains("complete") || value.contains("hoàn") -> COMPLETED
                value.contains("hiatus") || value.contains("paused") || value.contains("tạm") -> PAUSED
                value.contains("cancel") || value.contains("hủy") -> CANCELLED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Một bộ truyện nằm trên một nguồn (source) cụ thể.
 * [key] là định danh duy nhất toàn cục: "sourceId:mangaId".
 */
data class Manga(
    val id: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String = "",
    val author: String? = null,
    val description: String? = null,
    val status: MangaStatus = MangaStatus.UNKNOWN,
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val latestChapterName: String? = null,
    val updatedAt: Long = 0L,
) {
    val key: String get() = "$sourceId:$id"
}

/**
 * Một chương truyện.
 */
data class Chapter(
    val id: String,
    val mangaId: String,
    val sourceId: String,
    val name: String,
    val number: Float = 0f,
    val releasedAt: String? = null,
    val pageCount: Int = 0,
) {
    val key: String get() = "$sourceId:$mangaId:$id"
}

/**
 * Một trang truyện.
 *
 * [headers] là các header cần gửi kèm khi tải ảnh (một số nguồn yêu cầu Referer/Cookie).
 */
data class Page(
    val index: Int,
    val imageUrl: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * Chi tiết truyện kèm danh sách chương.
 */
data class MangaDetails(
    val manga: Manga,
    val chapters: List<Chapter> = emptyList(),
)

/**
 * Một mục trong lịch sử đọc.
 */
data class HistoryEntry(
    val manga: Manga,
    val chapter: Chapter,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val updatedAt: Long = 0L,
)
