package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.SManga

/**
 * Nguồn truyện có thể duyệt/tìm kiếm (hầu hết extension đều là loại này).
 */
@Suppress("Unused")
interface CatalogueSource : Source {

    /** Mã ngôn ngữ ISO 639-1 (hai chữ thường). */
    val lang: String

    val supportsRelatedMangas: Boolean get() = false

    val disableRelatedMangasBySearch: Boolean get() = false

    val disableRelatedMangas: Boolean get() = false

    suspend fun fetchRelatedMangaList(manga: SManga): List<SManga> =
        throw UnsupportedOperationException("Unsupported!")
}
