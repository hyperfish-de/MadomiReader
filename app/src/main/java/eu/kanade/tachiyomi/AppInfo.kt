package eu.kanade.tachiyomi

/**
 * Thông tin phiên bản của ứng dụng host, dùng cho các extension cần khai báo
 * User-Agent hoặc kiểm tra phiên bản.
 *
 * Bộ khung API này (`eu.kanade.tachiyomi.*`) là bản mô phỏng tương thích nhị phân với
 * `com.github.keiyoushi:extensions-lib` mà các extension của Keiyoushi/Mihon được biên dịch
 * dựa trên — chính ứng dụng này cung cấp phần thân thật (bản gốc chỉ là stub).
 */
object AppInfo {

    @Volatile
    private var versionCode: Int = 0

    @Volatile
    private var versionName: String = "1.0.0"

    /** Được gọi một lần lúc ứng dụng khởi động. */
    fun install(code: Int, name: String) {
        versionCode = code
        versionName = name
    }

    fun getVersionCode(): Int = versionCode

    fun getVersionName(): String = versionName
}
