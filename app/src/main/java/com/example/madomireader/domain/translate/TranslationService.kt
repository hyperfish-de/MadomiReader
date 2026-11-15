package com.example.madomireader.domain.translate

import com.example.madomireader.data.local.SettingsRepository
import com.example.madomireader.data.remote.gemini.GeminiTranslator
import com.example.madomireader.data.remote.gemini.MissingApiKeyException
import com.example.madomireader.domain.model.AppSettings
import com.example.madomireader.domain.model.GeminiModel
import com.example.madomireader.domain.model.OcrBlock
import com.example.madomireader.domain.model.SourceLanguage

/**
 * Ghép OCR với Gemini: nhận các khung thoại đã nhận diện, trả về bản dịch
 * tiếng Việt tương ứng từng khung (giữ nguyên thứ tự).
 */
class TranslationService(
    private val translator: GeminiTranslator,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun translateBlocks(
        blocks: List<OcrBlock>,
        storyContext: String? = null,
        settings: AppSettings? = null,
    ): Result<List<String>> {
        if (blocks.isEmpty()) return Result.success(emptyList())

        val current = settings ?: settingsRepository.snapshot()
        if (!current.hasApiKey) return Result.failure(MissingApiKeyException())

        val sources = blocks.map { it.text }
        return runCatching {
            val translated = translator.translateTexts(
                apiKey = current.apiKey,
                model = current.model,
                texts = sources,
                sourceLanguage = current.sourceLanguage,
                storyContext = storyContext,
            )
            translated.mapIndexed { index, value ->
                value.ifBlank { sources.getOrElse(index) { "" } }
            }
        }
    }

    /** Kiểm tra API key người dùng vừa nhập có hoạt động không. */
    suspend fun testApiKey(apiKey: String, model: GeminiModel): Result<String> = runCatching {
        val sample = translator.translateTexts(
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
