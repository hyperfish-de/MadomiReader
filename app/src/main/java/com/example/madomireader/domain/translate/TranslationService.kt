package com.example.madomireader.domain.translate

import com.example.madomireader.data.local.SettingsRepository
import com.example.madomireader.data.remote.gemini.GeminiTranslator
import com.example.madomireader.data.remote.gemini.MissingApiKeyException
import com.example.madomireader.data.remote.google.GoogleTranslator
import com.example.madomireader.data.remote.mlkit.MlKitTranslator
import com.example.madomireader.domain.model.AppSettings
import com.example.madomireader.domain.model.GeminiModel
import com.example.madomireader.domain.model.OcrBlock
import com.example.madomireader.domain.model.SourceLanguage
import com.example.madomireader.domain.model.TranslationEngine

/**
 * Dịch các khung thoại đã nhận diện sang tiếng Việt.
 *
 * Có ba công cụ dịch, chọn trong Cài đặt:
 *  - [TranslationEngine.GEMINI]: chất lượng cao nhất, cần API key.
 *  - [TranslationEngine.GOOGLE]: Google Dịch công khai, miễn phí, cần mạng.
 *  - [TranslationEngine.MLKIT]: dịch ngay trên máy, không cần key.
 */
class TranslationService(
    private val geminiTranslator: GeminiTranslator,
    private val googleTranslator: GoogleTranslator,
    private val mlKitTranslator: MlKitTranslator,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun translateBlocks(
        blocks: List<OcrBlock>,
        storyContext: String? = null,
        settings: AppSettings? = null,
    ): Result<List<String>> {
        if (blocks.isEmpty()) return Result.success(emptyList())

        val current = settings ?: settingsRepository.snapshot()
        val sources = blocks.map { it.text }

        return runCatching {
            val translated = when (current.translationEngine) {
                TranslationEngine.GEMINI -> translateWithGemini(current, sources, storyContext)
                TranslationEngine.GOOGLE -> googleTranslator.translateTexts(sources, current.sourceLanguage)
                TranslationEngine.MLKIT -> mlKitTranslator.translateTexts(sources, current.sourceLanguage)
            }
            translated.mapIndexed { index, value ->
                value.ifBlank { sources.getOrElse(index) { "" } }
            }
        }
    }

    private suspend fun translateWithGemini(
        current: AppSettings,
        sources: List<String>,
        storyContext: String?,
    ): List<String> {
        if (!current.hasApiKey) throw MissingApiKeyException()
        return geminiTranslator.translateTexts(
            apiKey = current.apiKey,
            model = current.model,
            texts = sources,
            sourceLanguage = current.sourceLanguage,
            storyContext = storyContext,
        )
    }

    /** Kiểm tra API key người dùng vừa nhập có hoạt động không (chỉ dùng cho Gemini). */
    suspend fun testApiKey(apiKey: String, model: GeminiModel): Result<String> = runCatching {
        val sample = geminiTranslator.translateTexts(
            apiKey = apiKey,
            model = model,
            texts = listOf("Hello! Nice to meet you."),
            sourceLanguage = SourceLanguage.ENGLISH,
        )
        sample.firstOrNull().orEmpty()
    }

    /** Ngôn ngữ gốc đang được cấu hình. */
    suspend fun currentSourceLanguage(): SourceLanguage = settingsRepository.snapshot().sourceLanguage
}
