package eu.kanade.tachiyomi.source

/**
 * Factory tạo nguồn lúc chạy — lớp vào của các extension APK mới
 * (ví dụ `keiyoushi.source.Generated`).
 */
@Suppress("Unused")
interface SourceFactory {

    /** Trả về danh sách nguồn mà extension này cung cấp. */
    fun createSources(): List<Source>
}
