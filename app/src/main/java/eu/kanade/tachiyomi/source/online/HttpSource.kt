package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.HostProvider
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

/**
 * Cài đặt cơ bản cho các nguồn truyện lấy dữ liệu từ website.
 *
 * Chữ ký phải khớp `extensions-lib` 1.6 (xem `library/api/library.api`), vì các extension
 * được biên dịch dựa trên đúng bộ chữ ký đó. Đặc biệt:
 *  - `headers` là thuộc tính uỷ nhiệm (`headers$delegate`) — `KeiSource` sửa trường này qua
 *    phản chiếu để bỏ tính "lazy".
 *  - `network` là `protected val` (getter final).
 *  - `headersBuilder()` và `imageRequest()` là `protected open`.
 */
@Suppress("Unused", "UnusedReceiverParameter")
abstract class HttpSource : CatalogueSource {

    /** Dịch vụ mạng dùng chung. */
    protected val network: NetworkHelper = NetworkHelper(HostProvider.context)

    /** URL gốc của website, không có dấu `/` ở cuối. */
    abstract val baseUrl: String

    /** URL trang chủ, dùng cho "Mở bằng trình duyệt". */
    open fun getHomeUrl(): String = baseUrl

    /**
     * Phiên bản của nguồn, dùng để sinh [id]. Tăng lên nếu website đổi cấu trúc URL.
     */
    open val versionId: Int = 1

    /**
     * ID của nguồn, sinh từ 8 byte đầu của MD5(`tên/ngôn ngữ/versionId`) với bit dấu bị xoá.
     */
    override val id: Long by lazy { generateId(name, lang, versionId) }

    /** Header dùng cho các request. */
    val headers: Headers by lazy { headersBuilder().build() }

    /** OkHttp client mặc định. */
    open val client: OkHttpClient
        get() = network.client

    /** Builder header mặc định; nguồn có thể ghi đè để thêm header riêng. */
    protected open fun headersBuilder(): Headers.Builder =
        Headers.Builder().add("User-Agent", HostProvider.userAgent)

    override fun toString(): String = name

    /**
     * Trả về URL ảnh của [page]. Chỉ được gọi khi `Page.imageUrl` là null.
     *
     * @since tachiyomix 1.6
     */
    open suspend fun getImageUrl(page: Page): String = page.imageUrl!!

    /**
     * Request lấy ảnh của trang. Ghi đè khi cần đổi URL, header hoặc dùng POST.
     */
    protected open fun imageRequest(page: Page): Request = Request.Builder()
        .url(page.imageUrl!!)
        .headers(headers)
        .build()

    /**
     * Gán URL chương mà bỏ scheme và domain (giữ được khi website đổi tên miền).
     */
    fun SChapter.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    /**
     * Gán URL truyện mà bỏ scheme và domain.
     */
    fun SManga.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    private fun getUrlWithoutDomain(orig: String): String = try {
        val uri = orig.toHttpUrlOrNull()
        if (uri != null) {
            buildString {
                append(uri.encodedPath)
                uri.encodedQuery?.let { append("?").append(it) }
            }
        } else {
            orig
        }
    } catch (_: Exception) {
        orig
    }

    /**
     * URL đầy đủ của truyện trên website.
     *
     * @since tachiyomix 1.4
     */
    abstract fun getMangaUrl(manga: SManga): String

    /**
     * URL đầy đủ của chương trên website.
     *
     * @since tachiyomix 1.4
     */
    abstract fun getChapterUrl(chapter: SChapter): String

    override fun getFilterList(): FilterList = FilterList()

    private companion object {
        fun generateId(name: String, lang: String, versionId: Int): Long {
            val key = "$name/$lang/$versionId"
            val md5 = MessageDigest.getInstance("MD5").digest(key.toByteArray())
            var id = 0L
            for (i in 0..7) {
                id = (id shl 8) or (md5[i].toLong() and 0xFF)
            }
            return id and Long.MAX_VALUE
        }
    }
}
