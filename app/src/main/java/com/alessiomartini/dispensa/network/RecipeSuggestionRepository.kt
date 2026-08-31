package com.alessiomartini.dispensa.network

import com.alessiomartini.dispensa.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface RecipeResult {
    data class Success(val recipes: List<RecipeSuggestion>) : RecipeResult
    data object NoApiKey : RecipeResult
    data class Error(val message: String) : RecipeResult
}

class RecipeSuggestionRepository(
    private val settingsRepository: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun suggestRecipes(pantryItemNames: List<String>): RecipeResult =
        withContext(Dispatchers.IO) {
            val settings = settingsRepository.settings.value
            if (settings.apiKey.isBlank()) return@withContext RecipeResult.NoApiKey

            val prompt = buildPrompt(pantryItemNames)
            val requestBody = json.encodeToString(
                AnthropicRequest.serializer(),
                AnthropicRequest(
                    model = settings.model,
                    max_tokens = 1500,
                    messages = listOf(AnthropicMessage(role = "user", content = prompt))
                )
            )

            val request = Request.Builder()
                .url(ANTHROPIC_API_URL)
                .addHeader("x-api-key", settings.apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = runCatching {
                            json.decodeFromString(AnthropicResponse.serializer(), bodyString).error?.message
                        }.getOrNull() ?: "HTTP ${response.code}"
                        return@withContext RecipeResult.Error(message)
                    }

                    val parsed = json.decodeFromString(AnthropicResponse.serializer(), bodyString)
                    val text = parsed.content.firstOrNull { it.type == "text" }?.text
                        ?: return@withContext RecipeResult.Error("Empty response from the model")

                    val recipes = parseRecipes(text)
                    RecipeResult.Success(recipes)
                }
            } catch (e: IOException) {
                RecipeResult.Error(e.message ?: "Network error")
            } catch (e: Exception) {
                RecipeResult.Error(e.message ?: "Unexpected error")
            }
        }

    private fun parseRecipes(rawText: String): List<RecipeSuggestion> {
        val cleaned = rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(RecipeSuggestion.serializer()),
            cleaned
        )
    }

    private fun buildPrompt(pantryItemNames: List<String>): String {
        val ingredients = pantryItemNames.joinToString(", ")
        return """
            I have these ingredients at home: $ingredients.

            Suggest 3 simple recipes I can make mostly with these ingredients (I can also use
            salt, pepper, oil, and water, which you can assume I have). Reply with ONLY a valid
            JSON array (no text before or after, no markdown), where each element has these fields:
            - "title": the recipe's name (string)
            - "ingredientsUsed": ingredients from my list used in this recipe (array of strings)
            - "missingIngredients": any ingredients the recipe needs that I don't have (array of strings, can be empty)
            - "steps": short preparation steps (array of strings)

            Write everything in English.
        """.trimIndent()
    }

    companion object {
        private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
    }
}
