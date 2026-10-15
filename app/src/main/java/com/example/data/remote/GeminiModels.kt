package com.example.data.remote

import com.squareup.moshi.JsonClass

/**
 * Request payload sent to Google Gemini generateContent and streamGenerateContent API endpoints.
 */
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null,
    val safetySettings: List<GeminiSafetySetting>? = null
) {
    companion object {
        /**
         * Convenience helper to create a simple text prompt request.
         */
        fun fromPrompt(text: String, role: String = "user"): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = role,
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            )
        }

        /**
         * Convenience helper with system instruction and user prompt.
         */
        fun withSystemInstruction(systemPrompt: String, userPrompt: String): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userPrompt))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                )
            )
        }
    }
}

/**
 * Turn/content within a Gemini conversation or prompt.
 */
@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

/**
 * Individual part of a Gemini Content object, supporting text and multimodal inlineData.
 */
@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

/**
 * Multimodal data payload (e.g. image or audio byte data encoded as base64).
 */
@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

/**
 * Generation configuration parameters for temperature, tokens, topP, topK, etc.
 */
@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null,
    val stopSequences: List<String>? = null
)

/**
 * Safety setting threshold definition.
 */
@JsonClass(generateAdapter = true)
data class GeminiSafetySetting(
    val category: String,
    val threshold: String
)

/**
 * Safety rating evaluation returned in candidate metadata.
 */
@JsonClass(generateAdapter = true)
data class GeminiSafetyRating(
    val category: String? = null,
    val probability: String? = null
)

/**
 * Response model received from Google Gemini API endpoints.
 */
@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val usageMetadata: GeminiUsageMetadata? = null,
    val modelVersion: String? = null,
    val error: GeminiError? = null
) {
    /**
     * Extracts the primary response text from the first candidate.
     */
    fun text(): String? = candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
}

/**
 * Individual generated candidate output.
 */
@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
    val index: Int? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null
)

/**
 * Token usage telemetry from Gemini API.
 */
@JsonClass(generateAdapter = true)
data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)

/**
 * Standard error representation returned by Gemini API on failed requests.
 */
@JsonClass(generateAdapter = true)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

// Standard type aliases matching Google Gemini naming conventions
typealias GenerateContentRequest = GeminiRequest
typealias GenerateContentResponse = GeminiResponse
typealias Content = GeminiContent
typealias Part = GeminiPart
typealias InlineData = GeminiInlineData
typealias GenerationConfig = GeminiGenerationConfig
typealias Candidate = GeminiCandidate
typealias UsageMetadata = GeminiUsageMetadata
