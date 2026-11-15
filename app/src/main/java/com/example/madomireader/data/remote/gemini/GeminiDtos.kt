package com.example.madomireader.data.remote.gemini

import com.google.gson.annotations.SerializedName

data class GeminiPart(
    @SerializedName("text") val text: String? = null,
)

data class GeminiContent(
    @SerializedName("role") val role: String? = null,
    @SerializedName("parts") val parts: List<GeminiPart> = emptyList(),
)

data class GeminiSystemInstruction(
    @SerializedName("parts") val parts: List<GeminiPart> = emptyList(),
)

data class GeminiGenerationConfig(
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("topP") val topP: Double? = null,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int? = null,
    @SerializedName("responseMimeType") val responseMimeType: String? = null,
    @SerializedName("responseSchema") val responseSchema: Map<String, Any>? = null,
)

data class GeminiRequest(
    @SerializedName("systemInstruction") val systemInstruction: GeminiSystemInstruction? = null,
    @SerializedName("contents") val contents: List<GeminiContent> = emptyList(),
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null,
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent? = null,
    @SerializedName("finishReason") val finishReason: String? = null,
)

data class GeminiError(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: String? = null,
)

data class GeminiPromptFeedback(
    @SerializedName("blockReason") val blockReason: String? = null,
)

data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>? = null,
    @SerializedName("error") val error: GeminiError? = null,
    @SerializedName("promptFeedback") val promptFeedback: GeminiPromptFeedback? = null,
)

data class TranslationItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("text") val text: String = "",
)

data class TranslationEnvelope(
    @SerializedName("translations") val translations: List<TranslationItem> = emptyList(),
)

/** Người dùng chưa nhập API key. */
class MissingApiKeyException : IllegalStateException(
    "Chưa có Gemini API Key. Vào Cài đặt để nhập API key của bạn.",
)

/** API Gemini trả về lỗi (hết quota, key sai, model không tồn tại…). */
class GeminiApiException(
    val code: Int,
    override val message: String,
) : IllegalStateException(message)
