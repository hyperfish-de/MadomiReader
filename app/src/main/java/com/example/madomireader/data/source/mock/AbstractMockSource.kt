package com.example.madomireader.data.source.mock

import com.example.madomireader.data.source.MangaSource
import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.domain.model.MangaDetails
import com.example.madomireader.domain.model.Page
import kotlinx.coroutines.delay

/**
 * Phần dùng chung của hai nguồn demo: tìm kiếm, lọc, sinh danh sách chương.
 * Lớp con chỉ cần quyết định ảnh bìa và nội dung trang truyện lấy từ đâu.
 */
abstract class AbstractMockSource : MangaSource {

    protected data class Entry(
        val mangaId: String,
        val index: Int,
        val seed: MockCatalog.MockSeed,
    )

    protected val entries: List<Entry> = MockCatalog.seeds
        .mapIndexed { index, seed -> Entry(mangaId = "m$index", index = index, seed = seed) }

    override fun genres(): List<String> = MockCatalog.genres

    override suspend fun browse(page: Int, query: String?, genre: String?): List<Manga> {
        delay(SIMULATED_LATENCY_MS)
        val keyword = query?.trim()?.lowercase().orEmpty()
        val filtered = entries.filter { entry ->
            val matchesKeyword = keyword.isEmpty() ||
                entry.seed.title.lowercase().contains(keyword) ||
                entry.seed.author.lowercase().contains(keyword) ||
                entry.seed.description.lowercase().contains(keyword)
            val matchesGenre = genre.isNullOrBlank() ||
                entry.seed.genres.any { it.equals(genre, ignoreCase = true) }
            matchesKeyword && matchesGenre
        }
        val from = (page.coerceAtLeast(0)) * PAGE_SIZE
        if (from >= filtered.size) return emptyList()
        return filtered.drop(from).take(PAGE_SIZE).map { entry -> entry.toManga() }
    }

    override suspend fun details(mangaId: String): MangaDetails {
        delay(SIMULATED_LATENCY_MS)
        val entry = findEntry(mangaId)
        return MangaDetails(manga = entry.toManga(), chapters = buildChapters(entry))
    }

    override suspend fun chapters(mangaId: String): List<Chapter> {
        delay(SIMULATED_LATENCY_MS)
        return buildChapters(findEntry(mangaId))
    }

    override suspend fun pages(chapterId: String): List<Page> {
        delay(SIMULATED_LATENCY_MS)
        val (mangaId, chapterNumber) = parseChapterId(chapterId)
        val entry = findEntry(mangaId)
        return buildPages(entry, chapterNumber)
    }

    /** Ảnh bìa của truyện. */
    protected abstract fun coverUrl(entry: Entry): String

    /** Nội dung các trang của một chương. */
    protected abstract fun buildPages(entry: Entry, chapterNumber: Int): List<Page>

    protected fun Entry.toManga(): Manga = Manga(
        id = mangaId,
        sourceId = id,
        title = seed.title,
        coverUrl = coverUrl(this),
        author = seed.author,
        description = seed.description,
        status = seed.status,
        genres = seed.genres,
        rating = 7.5 + (index % 25) * 0.1,
        latestChapterName = "Chương ${seed.chapterCount}",
        updatedAt = System.currentTimeMillis() - index * 3_600_000L,
    )

    protected fun findEntry(mangaId: String): Entry =
        entries.firstOrNull { it.mangaId == mangaId }
            ?: throw IllegalArgumentException("Không tìm thấy truyện $mangaId trên nguồn $id")

    private fun buildChapters(entry: Entry): List<Chapter> =
        (entry.seed.chapterCount downTo 1).map { number ->
            Chapter(
                id = "${entry.mangaId}-c$number",
                mangaId = entry.mangaId,
                sourceId = id,
                name = "Chương $number",
                number = number.toFloat(),
                releasedAt = "2025-${((number % 12) + 1).toString().padStart(2, '0')}-" +
                    "${((number % 27) + 1).toString().padStart(2, '0')}",
                pageCount = entry.seed.pagesPerChapter,
            )
        }

    /** Tách "m3-c12" thành ("m3", 12). */
    protected fun parseChapterId(chapterId: String): Pair<String, Int> {
        val separator = chapterId.lastIndexOf(CHAPTER_SEPARATOR)
        require(separator > 0) { "Mã chương không hợp lệ: $chapterId" }
        val mangaId = chapterId.substring(0, separator)
        val number = chapterId.substring(separator + CHAPTER_SEPARATOR.length).toIntOrNull() ?: 1
        return mangaId to number
    }

    /** Chọn vài câu hội thoại cho một trang, xoay vòng theo chương và số trang. */
    protected fun dialogueSlice(
        dialogue: List<String>,
        chapterNumber: Int,
        pageIndex: Int,
        count: Int,
    ): List<String> {
        if (dialogue.isEmpty()) return emptyList()
        val start = ((chapterNumber * 3) + pageIndex * 2) % dialogue.size
        return List(count) { offset -> dialogue[(start + offset) % dialogue.size] }
    }

    private companion object {
        const val PAGE_SIZE = 8
        const val SIMULATED_LATENCY_MS = 220L
        private const val CHAPTER_SEPARATOR = "-c"
    }
}
