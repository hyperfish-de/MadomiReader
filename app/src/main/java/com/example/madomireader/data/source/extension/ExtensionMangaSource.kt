package com.example.madomireader.data.source.extension

import com.example.madomireader.data.source.BrowseResult
import com.example.madomireader.data.source.MangaSource
import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.domain.model.MangaDetails
import com.example.madomireader.domain.model.MangaStatus
import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.SourceLanguage
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Cầu nối giữa một nguồn của extension (API `eu.kanade.tachiyomi.*`) và
 * [MangaSource] mà ứng dụng dùng.
 *
 * Vài điểm phải lưu ý:
 *  - `mangaId`/`chapterId` chính là `url` của `SManga`/`SChapter` (đúng như Mihon dùng),
 *    nên ta giữ lại `SManga`/`SChapter` gốc trong bộ nhớ đệm để gọi lại extension.
 *  - `KeiSource.getMangaUpdate` ném lỗi nếu gọi song song trên cùng `manga.url`,
 *    nên mọi lời gọi extension được xếp hàng qua [mutex].
 *  - URL ảnh lấy qua `imageRequest(page)` (phản chiếu) để có cả header cần thiết
 *    (Referer, Cookie…) khi tải ảnh bằng Coil.
 */
class ExtensionMangaSource(
    private val extensionSource: Source,
    private val extensionName: String,
) : MangaSource {

    override val id: String = "ext-${extensionSource.id}"

    override val name: String = extensionSource.name

    override val description: String =
        "Nguồn \"$name\" từ extension $extensionName."

    private val languageCode: String = (extensionSource as? CatalogueSource)?.lang.orEmpty()

    override val defaultLanguage: SourceLanguage = languageFromCode(languageCode)

    /** Xếp hàng các lời gọi extension: một số nguồn không chịu được gọi song song. */
    private val mutex = Mutex()

    private val mangaCache = ConcurrentHashMap<String, SManga>()
    private val chapterCache = ConcurrentHashMap<String, SChapter>()

    override fun genres(): List<String> = emptyList()

    override suspend fun browse(page: Int, query: String?, genre: String?): BrowseResult {
        val catalogue = extensionSource as? CatalogueSource
            ?: return BrowseResult(mangas = emptyList(), hasNextPage = false)

        // Mihon đếm trang từ 1, ứng dụng đếm từ 0.
        val mangasPage = mutex.withLock {
            if (query.isNullOrBlank()) {
                catalogue.getPopularManga(page + 1)
            } else {
                catalogue.getSearchManga(page + 1, query, FilterList())
            }
        }

        val mangas = mangasPage.mangas.map { smanga ->
            mangaCache[smanga.url] = smanga
            smanga.toManga()
        }
        return BrowseResult(mangas = mangas, hasNextPage = mangasPage.hasNextPage)
    }

    /**
     * Lấy thông tin truyện **và** danh sách chương trong một lời gọi.
     *
     * `MangaRepository` coi kết quả `details` là đã có chương, nên trả về cả hai ở đây
     * để tránh gọi mạng hai lần.
     */
    override suspend fun details(mangaId: String): MangaDetails {
        val update = mutex.withLock {
            extensionSource.getMangaUpdate(
                manga = mangaFor(mangaId),
                chapters = emptyList(),
                fetchDetails = true,
                fetchChapters = true,
            )
        }

        val manga = update.manga
        mangaCache[mangaId] = manga
        update.chapters.forEach { chapterCache[it.url] = it }

        return MangaDetails(
            manga = manga.toManga(),
            chapters = update.chapters.map { it.toChapter(mangaId) },
        )
    }

    override suspend fun chapters(mangaId: String): List<Chapter> {
        val update = mutex.withLock {
            extensionSource.getMangaUpdate(
                manga = mangaFor(mangaId),
                chapters = emptyList(),
                fetchDetails = false,
                fetchChapters = true,
            )
        }

        update.chapters.forEach { chapterCache[it.url] = it }
        return update.chapters.map { it.toChapter(mangaId) }
    }

    override suspend fun pages(chapterId: String): List<Page> {
        val chapter = chapterCache[chapterId] ?: SChapter.create().apply { url = chapterId }

        val extensionPages = mutex.withLock { extensionSource.getPageList(chapter) }
        val fallbackHeaders = (extensionSource as? HttpSource)?.headers

        // Đánh số lại trang từ 0 cho khớp với giao diện đọc truyện.
        return extensionPages.mapIndexed { index, extensionPage ->
            val request = resolveImageRequest(extensionPage)
            val imageUrl = request?.url?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: extensionPage.imageUrl?.takeIf { it.isNotBlank() }
                ?: extensionPage.url

            Page(
                index = index,
                imageUrl = imageUrl,
                headers = request?.headers?.toFlatMap()
                    ?: fallbackHeaders?.toFlatMap()
                    ?: emptyMap(),
            )
        }
    }

    // ------------------------------------------------------------------ phản chiếu

    /**
     * Gọi `imageRequest(page)` của extension (là `protected` trong `HttpSource`,
     * và bị `KeiSource` ghi đè thành `final`).
     *
     * Đi ngược lên cây lớp vì phương thức có thể nằm ở lớp cha do R8 đổi tên lớp.
     */
    private fun resolveImageRequest(
        page: eu.kanade.tachiyomi.source.model.Page,
    ): Request? {
        val source = extensionSource as? HttpSource ?: return null

        var clazz: Class<*>? = source.javaClass
        while (clazz != null) {
            try {
                val method = clazz.getDeclaredMethod(METHOD_IMAGE_REQUEST, PAGE_CLASS)
                if (method.returnType == Request::class.java) {
                    method.isAccessible = true
                    return method.invoke(source, page) as? Request
                }
            } catch (_: NoSuchMethodException) {
                // Không có ở lớp này, thử lớp cha.
            } catch (error: Throwable) {
                // Nguồn dùng POST hoặc tự xử lý ảnh: quay về URL của Page.
                return null
            }
            clazz = clazz.superclass
        }
        return null
    }

    private fun mangaFor(mangaId: String): SManga =
        mangaCache[mangaId] ?: SManga.create().apply { url = mangaId }

    // ------------------------------------------------------------------ chuyển đổi

    private fun SManga.toManga(): Manga = Manga(
        id = url,
        sourceId = id,
        title = title.ifBlank { url },
        coverUrl = thumbnail_url.orEmpty(),
        author = author?.takeIf { it.isNotBlank() } ?: artist,
        description = description,
        status = statusFromInt(status),
        genres = genre?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty(),
    )

    private fun SChapter.toChapter(mangaId: String): Chapter = Chapter(
        id = url,
        mangaId = mangaId,
        sourceId = id,
        name = name.ifBlank { url },
        number = chapter_number,
        releasedAt = formatDate(date_upload),
    )

    private fun formatDate(epochMillis: Long): String? {
        if (epochMillis <= 0L) return null
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))
        }.getOrNull()
    }

    private fun statusFromInt(status: Int): MangaStatus = when (status) {
        SManga.ONGOING -> MangaStatus.ONGOING
        SManga.COMPLETED -> MangaStatus.COMPLETED
        SManga.ON_HIATUS, SManga.PUBLISHING_FINISHED -> MangaStatus.PAUSED
        SManga.CANCELLED -> MangaStatus.CANCELLED
        else -> MangaStatus.UNKNOWN
    }

    private fun Headers.toFlatMap(): Map<String, String> =
        names().associateWith { headerName -> values(headerName).joinToString(", ") }

    private fun languageFromCode(code: String): SourceLanguage =
        when (code.lowercase(Locale.US).substringBefore('-')) {
            "en" -> SourceLanguage.ENGLISH
            "zh" -> SourceLanguage.CHINESE
            "ja" -> SourceLanguage.JAPANESE
            "ko" -> SourceLanguage.KOREAN
            else -> SourceLanguage.AUTO
        }

    override fun toString(): String = "ExtensionMangaSource($name, $id)"

    private companion object {
        const val METHOD_IMAGE_REQUEST = "imageRequest"

        val PAGE_CLASS: Class<*> = eu.kanade.tachiyomi.source.model.Page::class.java
    }
}
