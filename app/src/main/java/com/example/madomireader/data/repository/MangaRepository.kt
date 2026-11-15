package com.example.madomireader.data.repository

import com.example.madomireader.data.source.MangaSource
import com.example.madomireader.data.source.SourceRegistry
import com.example.madomireader.domain.model.Chapter
import com.example.madomireader.domain.model.Manga
import com.example.madomireader.domain.model.MangaDetails
import com.example.madomireader.domain.model.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Tầng truy cập dữ liệu cho UI: điều phối các [com.example.madomireader.data.source.MangaSource],
 * thêm cache trong bộ nhớ và chuẩn hoá lỗi thành [Result].
 */
class MangaRepository(
    private val registry: SourceRegistry,
) {

    private val detailsCache = LinkedHashMap<String, MangaDetails>()
    private val pagesCache = LinkedHashMap<String, List<Page>>()
    private val cacheMutex = Mutex()

    fun sources(): List<MangaSource> = registry.all

    fun genres(): List<String> = registry.genres()

    fun sourceName(sourceId: String): String = registry.find(sourceId)?.name ?: sourceId

    suspend fun browse(
        sourceId: String,
        page: Int,
        query: String? = null,
        genre: String? = null,
    ): Result<List<Manga>> = runCatchingSource {
        registry.require(sourceId).browse(page = page, query = query, genre = genre)
    }

    suspend fun details(sourceId: String, mangaId: String): Result<MangaDetails> {
        val cacheKey = "$sourceId:$mangaId"
        cacheMutex.withLock { detailsCache[cacheKey] }?.let { return Result.success(it) }

        return runCatchingSource {
            val details = registry.require(sourceId).details(mangaId)
            cacheMutex.withLock {
                detailsCache[cacheKey] = details
                trim(detailsCache)
            }
            details
        }
    }

    suspend fun chapters(sourceId: String, mangaId: String): Result<List<Chapter>> {
        val cacheKey = "$sourceId:$mangaId"
        cacheMutex.withLock { detailsCache[cacheKey] }?.let { return Result.success(it.chapters) }

        return runCatchingSource {
            val source = registry.require(sourceId)
            val chapters = source.chapters(mangaId)
            val cachedManga = cacheMutex.withLock { detailsCache[cacheKey]?.manga }
            val manga = cachedManga ?: source.details(mangaId).manga
            cacheMutex.withLock {
                detailsCache[cacheKey] = MangaDetails(manga = manga, chapters = chapters)
                trim(detailsCache)
            }
            chapters
        }
    }

    suspend fun pages(sourceId: String, chapterId: String): Result<List<Page>> {
        val cacheKey = "$sourceId:$chapterId"
        cacheMutex.withLock { pagesCache[cacheKey] }?.let { return Result.success(it) }

        return runCatchingSource {
            val pages = registry.require(sourceId).pages(chapterId)
            cacheMutex.withLock {
                pagesCache[cacheKey] = pages
                trim(pagesCache)
            }
            pages
        }
    }

    suspend fun clearCache() {
        cacheMutex.withLock {
            detailsCache.clear()
            pagesCache.clear()
        }
    }

    private suspend fun <T> runCatchingSource(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching { block() }
        }

    private fun <K, V> trim(cache: LinkedHashMap<K, V>) {
        while (cache.size > MAX_CACHE_ENTRIES) {
            val oldest = cache.keys.firstOrNull() ?: break
            cache.remove(oldest)
        }
    }

    private companion object {
        const val MAX_CACHE_ENTRIES = 40
    }
}
