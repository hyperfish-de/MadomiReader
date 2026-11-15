package com.example.madomireader.domain.model

/**
 * Chế độ đọc truyện.
 */
enum class ReaderMode(val id: String, val label: String) {
    VERTICAL("vertical", "Cuộn dọc (Webtoon/Manhwa)"),
    HORIZONTAL("horizontal", "Lật từng trang (Manga)");

    companion object {
        fun fromId(id: String?): ReaderMode =
            entries.firstOrNull { it.id == id } ?: VERTICAL
    }
}

/**
 * Ngôn ngữ gốc của truyện — quyết định bộ nhận diện chữ (OCR) nào sẽ chạy.
 */
enum class SourceLanguage(
    val id: String,
    val label: String,
    val promptName: String,
) {
    AUTO("auto", "Tự động nhận diện", "tự động nhận diện"),
    ENGLISH("en", "Tiếng Anh", "tiếng Anh"),
    CHINESE("zh", "Tiếng Trung", "tiếng Trung"),
    JAPANESE("ja", "Tiếng Nhật", "tiếng Nhật"),
    KOREAN("ko", "Tiếng Hàn", "tiếng Hàn");

    companion object {
        fun fromId(id: String?): SourceLanguage =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/**
 * Kiểu nền che chữ gốc khi vẽ lớp dịch.
 */
enum class OverlayStyle(val id: String, val label: String) {
    AUTO_SAMPLED("sampled", "Tự lấy màu nền quanh khung thoại"),
    WHITE_BOX("white", "Nền trắng, chữ đen"),
    BLACK_BOX("black", "Nền đen, chữ trắng");

    companion object {
        fun fromId(id: String?): OverlayStyle =
            entries.firstOrNull { it.id == id } ?: AUTO_SAMPLED
    }
}

/**
 * Model Gemini dùng để dịch.
 */
enum class GeminiModel(val id: String, val label: String) {
    FLASH("gemini-2.5-flash", "Gemini 2.5 Flash (chất lượng cao)"),
    FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite (nhanh, tiết kiệm)");

    companion object {
        fun fromId(id: String?): GeminiModel =
            entries.firstOrNull { it.id == id } ?: FLASH
    }
}

/**
 * Công cụ dịch bản dịch trên trang truyện.
 */
enum class TranslationEngine(
    val id: String,
    val label: String,
    val description: String,
) {
    MLKIT(
        "mlkit",
        "Dịch trên máy (ML Kit)",
        "Miễn phí, không cần mạng sau khi tải gói ngôn ngữ. Nhanh, chất lượng khá.",
    ),
    GOOGLE(
        "google",
        "Google Dịch (miễn phí)",
        "Không cần API key, cần mạng. Chất lượng tốt cho thoại ngắn.",
    ),
    GEMINI(
        "gemini",
        "Gemini AI (cần API key)",
        "Chất lượng cao nhất: hiểu ngữ cảnh truyện và nhân vật.",
    );

    companion object {
        fun fromId(id: String?): TranslationEngine =
            entries.firstOrNull { it.id == id } ?: GEMINI
    }
}

/**
 * Toàn bộ cài đặt người dùng, lưu trong DataStore Preferences.
 */
data class AppSettings(
    val apiKey: String = "",
    val model: GeminiModel = GeminiModel.FLASH,
    val translationEngine: TranslationEngine = TranslationEngine.GEMINI,
    val sourceLanguage: SourceLanguage = SourceLanguage.AUTO,
    val defaultReaderMode: ReaderMode = ReaderMode.VERTICAL,
    val overlayStyle: OverlayStyle = OverlayStyle.AUTO_SAMPLED,
    val autoTranslate: Boolean = false,
    val gridColumns: Int = 2,
    val fontSizeScale: Float = 1.0f,
    val extensionRepoUrl: String = DEFAULT_EXTENSION_REPO_URL,
) {
    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    /** Chỉ Gemini mới cần API key; hai công cụ còn lại chạy không cần key. */
    val requiresApiKey: Boolean get() = translationEngine == TranslationEngine.GEMINI

    companion object {
        /** Repo extension mặc định (Keiyoushi). */
        const val DEFAULT_EXTENSION_REPO_URL: String =
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json"
    }
}
