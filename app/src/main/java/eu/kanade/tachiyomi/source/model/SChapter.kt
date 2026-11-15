package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject

@Suppress("UNUSED", "PropertyName")
interface SChapter {

    var url: String

    var name: String

    var chapter_number: Float

    var scanlator: String?

    var date_upload: Long

    /**
     * Dữ liệu phụ do nguồn tự lưu, không hiển thị cho người dùng.
     *
     * @since tachiyomix 1.6
     */
    var memo: JsonObject

    companion object {
        fun create(): SChapter = SChapterImpl()
    }
}

/**
 * Cài đặt mặc định của [SChapter]. Hai chương trùng [url] được coi là một.
 */
class SChapterImpl : SChapter {

    override var url: String = ""

    override var name: String = ""

    override var chapter_number: Float = -1f

    override var scanlator: String? = null

    override var date_upload: Long = 0L

    override var memo: JsonObject = JsonObject(emptyMap())

    override fun equals(other: Any?): Boolean = other is SChapter && other.url == url

    override fun hashCode(): Int = url.hashCode()

    override fun toString(): String = "SChapterImpl(url='$url', name='$name')"
}
