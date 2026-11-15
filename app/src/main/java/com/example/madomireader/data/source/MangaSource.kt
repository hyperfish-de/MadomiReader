package com.example.madomireader.data.source

import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.domain.model.MangaDetails
import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.SourceLanguage

/**
 * Hợp đồng cho một "nguồn truyện" (MangaSource).
 *
 * Muốn thêm nguồn mới (MangaDex, TruyenQQ, NetTruyen…):
 *  1. Tạo một class implement interface này.
 *  2. Đăng ký nó trong `SourceRegistry`.
 * Không phải sửa gì ở tầng UI hay ViewModel.
 */
interface MangaSource {

    /** Định danh duy nhất, ví dụ "mangadex", "truyenqq". */
    val id: String

    /** Tên hiển thị cho người dùng. */
    val name: String

    /** Mô tả ngắn, hiện ở màn hình Cài đặt. */
    val description: String

    /** Ngôn ngữ gốc mặc định của nguồn — dùng làm gợi ý cho OCR. */
    val defaultLanguage: SourceLanguage

    /** Các thể loại nguồn này hỗ trợ. */
    fun genres(): List<String>

    /**
     * Duyệt / tìm truyện.
     *
     * @param page trang kết quả, bắt đầu từ 0.
     * @param query từ khoá tìm kiếm, null hoặc rỗng nghĩa là duyệt tất cả.
     * @param genre thể loại cần lọc, null nghĩa là tất cả.
     */
    suspend fun browse(page: Int, query: String?, genre: String?): List<Manga>

    /** Thông tin chi tiết một bộ truyện. */
    suspend fun details(mangaId: String): MangaDetails

    /** Danh sách chương của truyện. */
    suspend fun chapters(mangaId: String): List<Chapter>

    /** Danh sách trang ảnh của một chương. */
    suspend fun pages(chapterId: String): List<Page>
}

/**
 * Nơi giữ tất cả nguồn truyện đã đăng ký.
 */
class SourceRegistry(sources: List<MangaSource>) {

    val all: List<MangaSource> = sources

    private val byId: Map<String, MangaSource> = sources.associateBy { it.id }

    val default: MangaSource = sources.first()

    fun find(id: String?): MangaSource? = byId[id]

    fun require(id: String): MangaSource =
        byId[id] ?: throw IllegalArgumentException("Nguồn truyện không tồn tại: $id")

    fun genres(): List<String> = all.flatMap { it.genres() }.distinct().sorted()
}
