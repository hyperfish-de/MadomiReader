package com.example.madomireader.data.source

import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.domain.model.MangaDetails
import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.SourceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kết quả của một lần duyệt/tìm truyện.
 *
 * [hasNextPage] cho biết còn trang nữa hay không — nhờ vậy nguồn không phải đoán
 * "còn dữ liệu" chỉ vì trang vừa rồi trả về danh sách không rỗng.
 */
data class BrowseResult(
    val mangas: List<Manga>,
    val hasNextPage: Boolean,
)

/**
 * Hợp đồng cho một "nguồn truyện" (MangaSource).
 *
 * Có hai loại nguồn:
 *  - Nguồn viết sẵn trong ứng dụng (một class implement interface này).
 *  - Nguồn nạp từ APK extension của Keiyoushi/Tachiyomi
 *    (xem [com.example.madomireader.data.source.extension.ExtensionMangaSource]).
 *
 * Không phải sửa gì ở tầng UI hay ViewModel khi thêm nguồn mới.
 */
interface MangaSource {

    /** Định danh duy nhất, ví dụ "mangadex" hoặc "ext-1234567890". */
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
    suspend fun browse(page: Int, query: String?, genre: String?): BrowseResult

    /** Thông tin chi tiết một bộ truyện. */
    suspend fun details(mangaId: String): MangaDetails

    /** Danh sách chương của truyện. */
    suspend fun chapters(mangaId: String): List<Chapter>

    /** Danh sách trang ảnh của một chương. */
    suspend fun pages(chapterId: String): List<Page>
}

/**
 * Nơi giữ tất cả nguồn truyện đang dùng được.
 *
 * Danh sách này thay đổi lúc chạy: khi người dùng cài/gỡ một extension, ứng dụng
 * gọi [setAll] để cập nhật. UI quan sát [sources] để tự vẽ lại.
 */
class SourceRegistry {

    private val _sources = MutableStateFlow<List<MangaSource>>(emptyList())

    /** Dòng nguồn truyện hiện tại, để UI thu thập. */
    val sources: StateFlow<List<MangaSource>> = _sources.asStateFlow()

    private val _initialized = MutableStateFlow(false)

    /**
     * Đã nạp xong các extension đã cài hay chưa.
     *
     * Lúc mới mở ứng dụng, [sources] rỗng trong giây lát (đang đọc APK extension), nên UI
     * cần phân biệt "chưa nạp xong" với "thật sự chưa có nguồn nào".
     */
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    val all: List<MangaSource> get() = _sources.value

    fun setAll(sources: List<MangaSource>) {
        _sources.value = sources
        _initialized.value = true
    }

    fun find(id: String?): MangaSource? =
        if (id == null) null else all.firstOrNull { it.id == id }

    fun require(id: String): MangaSource =
        find(id) ?: throw IllegalArgumentException("Nguồn truyện không tồn tại: $id")

    fun genres(): List<String> = all.flatMap { it.genres() }.distinct().sorted()
}
