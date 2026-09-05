package com.alessiomartini.dispensa.network

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null,
    val responseSchema: GeminiSchema? = null
)

/**
 * Subset of the OpenAPI schema Gemini's structured-output mode accepts. Passing this makes the
 * model do constrained decoding against the exact shape instead of free-form JSON, which is what
 * actually prevents malformed output (e.g. an unescaped quote inside a title breaking the parser)
 * - responseMimeType alone only asks for JSON, it doesn't enforce a shape.
 */
@Serializable
data class GeminiSchema(
    val type: String,
    val items: GeminiSchema? = null,
    val properties: Map<String, GeminiSchema>? = null,
    val required: List<String>? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val error: GeminiError? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class RecipeSuggestion(
    val title: String,
    val ingredientsUsed: List<String> = emptyList(),
    val missingIngredients: List<String> = emptyList(),
    val steps: List<String> = emptyList()
)
