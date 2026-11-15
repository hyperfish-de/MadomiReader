package com.example.madomireader.data.remote.google

import com.example.madomireader.domain.model.SourceLanguage
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Dịch bằng điểm cuối công khai của Google Dịch (`translate_a/single`).
 *
 * Không cần API key. Mỗi đoạn thoại là một request — một trang truyện thường chỉ có
 * vài khung nên số request vẫn nhỏ.
 */
class GoogleTranslator(private val client: OkHttpClient) {

    suspend fun translateTexts(
        texts: List<String>,
        sourceLanguage: SourceLanguage,
    ): List<String> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyList()

        val sourceCode = sourceCodeFor(sourceLanguage)
        texts.map { text -> translateOne(text, sourceCode) }
    }

    private fun translateOne(text: String, sourceCode: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return text

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", sourceCode)
            .addQueryParameter("tl", TARGET_LANGUAGE)
            .addQueryParameter("dt", "t")
            .addQueryParameter("q", trimmed)
            .build()

        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Google Dịch trả về HTTP ${response.code}")
            }
            return parseTranslation(response.body?.string().orEmpty())
        }
    }

    /**
     * Dạng trả về:
     * `[[["bản dịch","nguồn",null,null,10],["phần 2",…]],null,"en",…]`
     */
    private fun parseTranslation(body: String): String {
        val root = JsonParser.parseString(body)
        if (!root.isJsonArray) throw IOException("Google Dịch trả về dữ liệu không hợp lệ")

        val segments = root.asJsonArray.firstOrNull()
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?: return ""

        val builder = StringBuilder()
        segments.forEach { segment ->
            if (!segment.isJsonArray) return@forEach
            val part = segment.asJsonArray.firstOrNull() ?: return@forEach
            if (part.isJsonPrimitive) builder.append(part.asString)
        }
        return builder.toString().trim()
    }

    private fun sourceCodeFor(language: SourceLanguage): String = when (language) {
        SourceLanguage.AUTO -> "auto"
        SourceLanguage.ENGLISH -> "en"
        SourceLanguage.CHINESE -> "zh-CN"
        SourceLanguage.JAPANESE -> "ja"
        SourceLanguage.KOREAN -> "ko"
    }

    private companion object {
        const val BASE_URL = "https://translate.googleapis.com/translate_a/single"
        const val TARGET_LANGUAGE = "vi"
    }
}
