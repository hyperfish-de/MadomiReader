package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate

/**
 * Hợp đồng cơ bản của một nguồn truyện (extension).
 *
 * Chữ ký ở đây phải khớp chính xác với `extensions-lib` phiên bản 1.6 của Keiyoushi,
 * vì các extension APK được biên dịch dựa trên đúng bộ chữ ký này.
 */
@Suppress("Unused")
interface Source {

    /** ID của nguồn. Phải là duy nhất. */
    val id: Long

    /** Tên nguồn. */
    val name: String

    /** Nguồn có hỗ trợ mục "mới cập nhật" hay không. */
    val supportsLatest: Boolean

    fun getFilterList(): FilterList = throw Exception("Stub!")

    suspend fun getPopularManga(page: Int): MangasPage

    suspend fun getLatestUpdates(page: Int): MangasPage

    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage

    suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate

    suspend fun getPageList(chapter: SChapter): List<Page>
}
