package com.example.madomireader.data.remote.gemini

import android.util.Log
import com.example.madomireader.domain.model.GeminiModel
import com.example.madomireader.domain.model.SourceLanguage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gọi trực tiếp REST API của Google Gemini để dịch hội thoại trong truyện tranh.
 *
 * Không dùng SDK nặng: chỉ OkHttp + Gson, đúng một endpoint
 * `POST /v1beta/models/{model}:generateContent`.
 */
class GeminiTranslator(
    private val client: OkHttpClient,
    private val gson: Gson,
) {

    /**
     * Dịch danh sách đoạn hội thoại sang [targetLanguage], trả về danh sách cùng thứ tự.
     *
     * @throws MissingApiKeyException khi [apiKey] rỗng.
     * @throws GeminiApiException khi API trả lỗi.
     */
    suspend fun translateTexts(
        apiKey: String,
        model: GeminiModel,
        texts: List<String>,
        sourceLanguage: SourceLanguage,
        storyContext: String? = null,
        targetLanguage: String = "tiếng Việt",
    ): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw MissingApiKeyException()
        if (texts.isEmpty()) return@withContext emptyList<String>()

        val output = ArrayList<String>(texts.size)
        texts.chunked(MAX_TEXTS_PER_REQUEST).forEach { chunk ->
            output += translateChunk(
                apiKey = apiKey,
                model = model,
                texts = chunk,
                sourceLanguage = sourceLanguage,
                storyContext = storyContext,
                targetLanguage = targetLanguage,
            )
        }
        output
    }

    private suspend fun translateChunk(
        apiKey: String,
        model: GeminiModel,
        texts: List<String>,
        sourceLanguage: SourceLanguage,
        storyContext: String?,
        targetLanguage: String,
    ): List<String> {
        val payload = GeminiRequest(
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = buildSystemPrompt(targetLanguage))),
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(
                            text = buildUserPrompt(texts, sourceLanguage, storyContext, targetLanguage),
                        ),
                    ),
                ),
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.25,
                topP = 0.95,
                maxOutputTokens = 8192,
                responseMimeType = "application/json",
                responseSchema = buildResponseSchema(),
            ),
        )

        val requestBody = gson.toJson(payload).toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL/models/${model.id}:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val raw = executeWithRetry(request)
        val response = gson.fromJson(raw, GeminiResponse::class.java)

        response.error?.let { error ->
            throw GeminiApiException(
                code = error.code,
                message = error.message ?: "Gemini trả về lỗi không xác định",
            )
        }

        response.promptFeedback?.blockReason?.let { reason ->
            throw GeminiApiException(400, "Yêu cầu bị chặn: $reason")
        }

        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("")
            ?.trim()
            .orEmpty()

        if (text.isEmpty()) {
            throw GeminiApiException(502, "Gemini không trả về nội dung nào")
        }

        return parseTranslations(text, texts.size)
    }

    private suspend fun executeWithRetry(request: Request, maxAttempts: Int = 3): String {
        var lastError: Exception? = null
        var delayMs = 700L

        repeat(maxAttempts) { attempt ->
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) return body

                    val message = extractErrorMessage(body)
                    val retryable = response.code == 429 || response.code >= 500
                    if (!retryable) {
                        throw GeminiApiException(response.code, message)
                    }
                    lastError = GeminiApiException(response.code, message)
                }
            } catch (error: GeminiApiException) {
                throw error
            } catch (error: IOException) {
                lastError = error
            }

            if (attempt < maxAttempts - 1) {
                delay(delayMs)
                delayMs *= 2
            }
        }

        throw lastError ?: GeminiApiException(500, "Không gọi được Gemini API")
    }

    private fun extractErrorMessage(body: String): String {
        if (body.isBlank()) return "Gemini trả về lỗi không có nội dung"
        return runCatching {
            val error = gson.fromJson(body, GeminiResponse::class.java)?.error
            error?.message ?: body.take(300)
        }.getOrElse { body.take(300) }
    }

    /**
     * Đọc kết quả JSON của Gemini. Có 3 lớp dự phòng để không bao giờ mất bản dịch.
     */
    fun parseTranslations(raw: String, expectedCount: Int): List<String> {
        // 1) JSON đúng schema.
        runCatching {
            val envelope = gson.fromJson(raw, TranslationEnvelope::class.java)
            val map = envelope?.translations?.associate { it.id to it.text }.orEmpty()
            if (map.isNotEmpty()) {
                val oneBased = !map.containsKey(0) && map.containsKey(1)
                return (0 until expectedCount).map { index ->
                    map[if (oneBased) index + 1 else index] ?: ""
                }
            }
        }

        // 2) Trích mảng JSON nằm trong văn bản (trường hợp model thêm lời dẫn).
        val arrayStart = raw.indexOf('[')
        val arrayEnd = raw.lastIndexOf(']')
        if (arrayStart in 0 until arrayEnd) {
            val candidate = raw.substring(arrayStart, arrayEnd + 1)
            runCatching {
                val array: JsonArray? = gson.fromJson(candidate, JsonArray::class.java)
                if (array != null && array.size() > 0) {
                    val values: List<String> = array.map { element: JsonElement ->
                        if (element.isJsonObject) {
                            val obj: JsonObject = element.asJsonObject
                            obj.get("text")?.let { text -> runCatching { text.asString }.getOrNull() }
                                ?: element.toString()
                        } else {
                            runCatching { element.asString }.getOrNull() ?: element.toString()
                        }
                    }
                    if (values.size >= expectedCount) {
                        return values.take(expectedCount)
                    }
                }
            }
        }

        // 3) Mỗi dòng là một bản dịch.
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size == expectedCount) return lines

        Log.w(TAG, "Không đọc được JSON từ Gemini, trả về danh sách rỗng. Độ dài: ${raw.length}")
        return List(expectedCount) { "" }
    }

    private fun buildSystemPrompt(targetLanguage: String): String = """
        Bạn là dịch giả truyện tranh (manga / manhwa / manhua) chuyên nghiệp, dịch sang $targetLanguage.

        Nguyên tắc bắt buộc:
        1. Giữ đúng sắc thái hội thoại: câu nói của nhân vật phải nghe tự nhiên như người Việt nói, tuyệt đối không dịch máy móc từng chữ.
        2. Chọn cách xưng hô phù hợp quan hệ nhân vật và ngữ cảnh (tôi - cậu, anh - em, tao - mày, ta - ngươi, huynh - muội, tỷ - đệ, ngài - ta…). Nếu không rõ quan hệ thì dùng trung tính: tôi - bạn.
        3. Giữ nguyên tên riêng, tên địa danh, tên chiêu thức; với tên người Trung Quốc có thể dùng phiên âm Hán - Việt.
        4. Giữ nguyên từ tượng thanh và tiếng hét (ví dụ: Rầm!, Bốp!, Á!, Hự!, Keng!).
        5. Không thêm chú thích, không giải thích, không thêm ngoặc đơn, không bọc câu trong dấu ngoặc kép.
        6. Câu ngắn thì dịch ngắn. Không gộp hai khung thoại khác nhau thành một câu.
        7. Nếu một đoạn bị lỗi hoặc vô nghĩa, trả lại nguyên văn đoạn đó.
        8. Chỉ trả về JSON đúng schema đã cho, không kèm bất kỳ văn bản nào khác.
    """.trimIndent()

    private fun buildUserPrompt(
        texts: List<String>,
        sourceLanguage: SourceLanguage,
        storyContext: String?,
        targetLanguage: String,
    ): String {
        val builder = StringBuilder()
        builder.append("Ngôn ngữ gốc của truyện: ").append(sourceLanguage.promptName).append('\n')
        if (!storyContext.isNullOrBlank()) {
            builder.append("Ngữ cảnh truyện: ").append(storyContext.trim()).append('\n')
        }
        builder.append('\n')
        builder.append("Dịch ").append(texts.size)
            .append(" đoạn hội thoại dưới đây sang ")
            .append(targetLanguage)
            .append(".\n")
        builder.append("Trả về JSON dạng {\"translations\":[{\"id\":<số thứ tự>,\"text\":\"<bản dịch>\"}]}.\n")
        builder.append("Giữ đúng số thứ tự id như danh sách dưới đây.\n\n")
        texts.forEachIndexed { index, text ->
            builder.append(index).append(". ").append(text.replace('\n', ' ').trim()).append('\n')
        }
        return builder.toString()
    }

    private fun buildResponseSchema(): Map<String, Any> = linkedMapOf(
        "type" to "object",
        "properties" to linkedMapOf(
            "translations" to linkedMapOf(
                "type" to "array",
                "items" to linkedMapOf(
                    "type" to "object",
                    "properties" to linkedMapOf(
                        "id" to linkedMapOf("type" to "integer"),
                        "text" to linkedMapOf("type" to "string"),
                    ),
                    "required" to listOf("id", "text"),
                    "propertyOrdering" to listOf("id", "text"),
                ),
            ),
        ),
        "required" to listOf("translations"),
        "propertyOrdering" to listOf("translations"),
    )

    companion object {
        private const val TAG = "GeminiTranslator"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MAX_TEXTS_PER_REQUEST = 30

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Client OkHttp dùng chung, có timeout dài vì model có thể "suy nghĩ" lâu. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
