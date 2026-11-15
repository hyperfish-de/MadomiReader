package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject

@Suppress("UNUSED", "PropertyName")
interface SManga {

    var url: String

    var title: String

    var thumbnail_url: String?

    var artist: String?

    var author: String?

    var status: Int

    var description: String?

    var genre: String?

    var update_strategy: UpdateStrategy

    /**
     * Dữ liệu phụ do nguồn tự lưu, không hiển thị cho người dùng.
     *
     * @since tachiyomix 1.6
     */
    var memo: JsonObject

    var initialized: Boolean

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SManga = SMangaImpl()
    }
}

/**
 * Cài đặt mặc định của [SManga].
 *
 * Hai đối tượng được coi là cùng một truyện nếu trùng [url] — đúng như Mihon, nhờ vậy
 * các phép so sánh/khử trùng lặp trong nguồn hoạt động như mong đợi.
 */
class SMangaImpl : SManga {

    override var url: String = ""

    override var title: String = ""

    override var thumbnail_url: String? = null

    override var artist: String? = null

    override var author: String? = null

    override var status: Int = SManga.UNKNOWN

    override var description: String? = null

    override var genre: String? = null

    override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE

    override var memo: JsonObject = JsonObject(emptyMap())

    override var initialized: Boolean = false

    override fun equals(other: Any?): Boolean = other is SManga && other.url == url

    override fun hashCode(): Int = url.hashCode()

    override fun toString(): String = "SMangaImpl(url='$url', title='$title')"
}
