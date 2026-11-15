package com.example.madomireader.data.source.mock

import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.SourceLanguage

/**
 * Nguồn mẫu thứ hai: trang truyện được tải từ mạng, dùng để kiểm thử
 * Coil (tải ảnh, cache đĩa, nút "Thử lại" khi lỗi mạng) và luồng đọc thông thường.
 */
class OnlineDemoSource : AbstractMockSource() {

    override val id: String = "online-demo"

    override val name: String = "Madomi Online Demo"

    override val description: String =
        "Nguồn mẫu thứ hai: ảnh trang truyện được tải từ mạng, dùng để kiểm thử tải ảnh, " +
            "cache cục bộ và nút thử lại khi mất mạng."

    override val defaultLanguage: SourceLanguage = SourceLanguage.ENGLISH

    override fun coverUrl(entry: Entry): String =
        "https://picsum.photos/seed/madomi-online-cover-${entry.index}/400/560"

    override fun buildPages(entry: Entry, chapterNumber: Int): List<Page> =
        (0 until entry.seed.pagesPerChapter).map { pageIndex ->
            Page(
                index = pageIndex,
                imageUrl = "https://picsum.photos/seed/" +
                    "madomi-${entry.mangaId}-$chapterNumber-$pageIndex/800/1200",
                width = 800,
                height = 1200,
            )
        }
}
