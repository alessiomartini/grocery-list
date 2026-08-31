package com.alessiomartini.dispensa.network

import kotlinx.serialization.Serializable

@Serializable
data class AnthropicRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<AnthropicMessage>
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
    val error: AnthropicError? = null
)

@Serializable
data class AnthropicContentBlock(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class AnthropicError(
    val type: String? = null,
    val message: String? = null
)

@Serializable
data class RecipeSuggestion(
    val title: String,
    val ingredientsUsed: List<String> = emptyList(),
    val missingIngredients: List<String> = emptyList(),
    val steps: List<String> = emptyList()
)
