package com.example.madomireader.data.source.mock

import com.example.madomireader.domain.model.Page
import com.example.madomireader.domain.model.SourceLanguage

/**
 * Nguồn mẫu chạy hoàn toàn offline.
 *
 * Các trang truyện không được tải về mà được vẽ ngay trên máy
 * (xem `PlaceholderPageRenderer`), nên có thể thử luồng
 * "OCR → dịch → vẽ đè bản dịch" mà không cần mạng.
 */
class OfflineDemoSource : AbstractMockSource() {

    override val id: String = "offline-demo"

    override val name: String = "Madomi Offline Demo"

    override val description: String =
        "Nguồn mẫu không cần mạng: các trang truyện được vẽ tại chỗ với hội thoại tiếng Anh/Trung, " +
            "dùng để thử tính năng nhận diện chữ và dịch ngay trên trang truyện."

    override val defaultLanguage: SourceLanguage = SourceLanguage.AUTO

    override fun coverUrl(entry: Entry): String =
        "https://picsum.photos/seed/madomi-cover-${entry.index}/400/560"

    override fun buildPages(entry: Entry, chapterNumber: Int): List<Page> =
        (0 until entry.seed.pagesPerChapter).map { pageIndex ->
            val lineCount = 2 + (pageIndex % 2)
            Page(
                index = pageIndex,
                imageUrl = "",
                width = 800,
                height = 1200,
                placeholderLines = dialogueSlice(
                    dialogue = entry.seed.dialogue,
                    chapterNumber = chapterNumber,
                    pageIndex = pageIndex,
                    count = lineCount,
                ),
            )
        }
}
