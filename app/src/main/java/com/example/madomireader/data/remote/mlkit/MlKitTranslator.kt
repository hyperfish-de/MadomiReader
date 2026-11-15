package com.example.madomireader.data.remote.mlkit

import com.example.madomireader.core.util.awaitTask
import com.example.madomireader.domain.model.SourceLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dịch ngay trên máy bằng ML Kit Translate.
 *
 * ML Kit cần biết chắc ngôn ngữ gốc, nên khi người dùng để "Tự động" thì ở đây
 * chạy thêm ML Kit Language ID để đoán ngôn ngữ từ chính nội dung trang.
 * Gói ngôn ngữ được tải về máy ở lần dùng đầu tiên (cần mạng).
 */
class MlKitTranslator {

    private val languageIdentifier: LanguageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    suspend fun translateTexts(
        texts: List<String>,
        sourceLanguage: SourceLanguage,
    ): List<String> = withContext(Dispatchers.Default) {
        if (texts.isEmpty()) return@withContext emptyList()

        val sourceCode = resolveSourceLanguage(sourceLanguage, texts)
        val translator = createTranslator(sourceCode)

        try {
            translator.downloadModelIfNeeded().awaitTask()
            texts.map { text ->
                val trimmed = text.trim()
                if (trimmed.isEmpty()) text else translator.translate(trimmed).awaitTask()
            }
        } finally {
            translator.close()
        }
    }

    /** Ngôn ngữ gốc thực sự dùng để dịch (đã giải quyết chế độ "Tự động"). */
    private suspend fun resolveSourceLanguage(
        sourceLanguage: SourceLanguage,
        texts: List<String>,
    ): String {
        if (sourceLanguage != SourceLanguage.AUTO) return mlKitCode(sourceLanguage)

        val sample = texts.joinToString(" ").take(MAX_SAMPLE_LENGTH)
        if (sample.isBlank()) return TranslateLanguage.ENGLISH

        val detected = languageIdentifier.identifyLanguage(sample).awaitTask()
        return mlKitCodeOrNull(detected) ?: throw IllegalStateException(
            "Không nhận diện được ngôn ngữ của truyện. " +
                "Hãy chọn ngôn ngữ gốc trong Cài đặt rồi thử lại.",
        )
    }

    private fun createTranslator(sourceCode: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode)
            .setTargetLanguage(TranslateLanguage.VIETNAMESE)
            .build()
        return Translation.getClient(options)
    }

    private fun mlKitCode(language: SourceLanguage): String = when (language) {
        SourceLanguage.ENGLISH -> TranslateLanguage.ENGLISH
        SourceLanguage.CHINESE -> TranslateLanguage.CHINESE
        SourceLanguage.JAPANESE -> TranslateLanguage.JAPANESE
        SourceLanguage.KOREAN -> TranslateLanguage.KOREAN
        SourceLanguage.AUTO -> TranslateLanguage.ENGLISH
    }

    private fun mlKitCodeOrNull(tag: String?): String? =
        when (tag?.lowercase()?.substringBefore('-')) {
            "en" -> TranslateLanguage.ENGLISH
            "zh" -> TranslateLanguage.CHINESE
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            else -> null
        }

    private companion object {
        const val MAX_SAMPLE_LENGTH = 400
    }
}
