package com.alessiomartini.dispensa.network

import com.alessiomartini.dispensa.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

enum class RecipeType {
    MEAL,
    SNACK
}

/**
 * Uses the Gemini API (Google AI Studio) to suggest recipes, since it has a genuinely free tier
 * for personal-scale use - see console.aistudio.google.com. Same JSON-array response contract
 * as before; only the request/response shape and endpoint are Gemini-specific.
 */
class RecipeSuggestionRepository(
    private val settingsRepository: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val recipeListSchema = GeminiSchema(
        type = "ARRAY",
        items = GeminiSchema(
            type = "OBJECT",
            properties = mapOf(
                "title" to GeminiSchema(type = "STRING"),
                "ingredientsUsed" to GeminiSchema(type = "ARRAY", items = GeminiSchema(type = "STRING")),
                "missingIngredients" to GeminiSchema(type = "ARRAY", items = GeminiSchema(type = "STRING")),
                "steps" to GeminiSchema(type = "ARRAY", items = GeminiSchema(type = "STRING"))
            ),
            required = listOf("title", "ingredientsUsed", "missingIngredients", "steps")
        )
    )

    suspend fun suggestRecipes(pantryItemNames: List<String>, type: RecipeType): RecipeResult =
        withContext(Dispatchers.IO) {
            val settings = settingsRepository.settings.value
            if (settings.apiKey.isBlank()) return@withContext RecipeResult.NoApiKey

            val prompt = buildPrompt(pantryItemNames, type)
            val requestBody = json.encodeToString(
                GeminiRequest.serializer(),
                GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(
                        maxOutputTokens = 2048,
                        responseMimeType = "application/json",
                        responseSchema = recipeListSchema
                    )
                )
            )

            // The free tier rate-limits each model separately, so when the configured one is
            // saturated another Flash model usually still answers. A model that no longer exists
            // (Google retires them) is skipped the same way instead of breaking recipes for good.
            val models = (listOf(settings.model) + FALLBACK_MODELS)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            var lastBusyMessage: String? = null
            var lastMissingModelMessage: String? = null
            for (model in models) {
                for (attempt in 1..ATTEMPTS_PER_MODEL) {
                    when (val result = callModel(model, settings.apiKey, requestBody)) {
                        is Attempt.Ok -> return@withContext parseResponse(result.body)
                        is Attempt.Fatal -> return@withContext RecipeResult.Error(result.message)
                        is Attempt.NoSuchModel -> {
                            lastMissingModelMessage = result.message
                            break
                        }
                        is Attempt.Busy -> {
                            lastBusyMessage = result.message
                            val wait = result.retryAfterMs ?: DEFAULT_RETRY_DELAY_MS
                            // A long cooldown means the quota is spent for a while: moving on to
                            // the next model beats leaving the spinner up.
                            if (attempt == ATTEMPTS_PER_MODEL || wait > MAX_RETRY_DELAY_MS) break
                            delay(wait)
                        }
                    }
                }
            }

            RecipeResult.Error(
                when {
                    lastBusyMessage != null ->
                        "Gemini is busy right now - the free tier only allows a few requests per minute, " +
                            "and every model tried was at its limit. Wait a minute and try again. ($lastBusyMessage)"
                    else -> lastMissingModelMessage ?: "No Gemini model available"
                }
            )
        }

    private sealed interface Attempt {
        data class Ok(val body: String) : Attempt
        /** Rate-limited (429) or overloaded (5xx): worth retrying, or trying another model. */
        data class Busy(val message: String, val retryAfterMs: Long?) : Attempt
        /** 404: this model isn't available to this key (any more) - try the next one. */
        data class NoSuchModel(val message: String) : Attempt
        /** Anything else (bad key, bad request, no network): retrying won't help. */
        data class Fatal(val message: String) : Attempt
    }

    private fun callModel(model: String, apiKey: String, requestBody: String): Attempt {
        val request = Request.Builder()
            .url("$GEMINI_API_BASE_URL/$model:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("content-type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) return Attempt.Ok(bodyString)

                val message = runCatching {
                    json.decodeFromString(GeminiResponse.serializer(), bodyString).error?.message
                }.getOrNull() ?: "HTTP ${response.code}"
                when (response.code) {
                    429, 500, 502, 503, 504 ->
                        Attempt.Busy(message, retryAfterMs(response.header("Retry-After"), bodyString))
                    404 -> Attempt.NoSuchModel(message)
                    else -> Attempt.Fatal(message)
                }
            }
        } catch (e: IOException) {
            Attempt.Fatal(e.message ?: "Network error")
        } catch (e: IllegalArgumentException) {
            // OkHttp rejects a malformed URL, e.g. a model name with spaces typed in Settings.
            Attempt.Fatal(e.message ?: "Invalid model name")
        }
    }

    /**
     * How long Gemini asks us to wait: the standard Retry-After header if present, otherwise the
     * google.rpc.RetryInfo it puts in a 429's error details (e.g. "retryDelay": "17s").
     */
    private fun retryAfterMs(retryAfterHeader: String?, body: String): Long? {
        retryAfterHeader?.trim()?.toLongOrNull()?.let { return it * 1000 }
        return runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
                ?.get("details")?.jsonArray
                ?.firstNotNullOfOrNull { it.jsonObject["retryDelay"]?.jsonPrimitive?.content }
                ?.removeSuffix("s")
                ?.toDouble()
                ?.let { (it * 1000).toLong() }
        }.getOrNull()
    }

    private fun parseResponse(body: String): RecipeResult = try {
        val candidate = json.decodeFromString(GeminiResponse.serializer(), body).candidates.firstOrNull()
        val text = candidate?.content?.parts?.firstOrNull()?.text
        if (text == null) {
            RecipeResult.Error("Empty response from the model")
        } else {
            try {
                RecipeResult.Success(parseRecipes(text))
            } catch (e: Exception) {
                if (candidate?.finishReason == "MAX_TOKENS") {
                    RecipeResult.Error("The response got cut off before finishing - try again with fewer pantry items")
                } else {
                    RecipeResult.Error(e.message ?: "Couldn't read the model's answer")
                }
            }
        }
    } catch (e: Exception) {
        RecipeResult.Error(e.message ?: "Unexpected error")
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

    private fun buildPrompt(pantryItemNames: List<String>, type: RecipeType): String {
        val ingredients = pantryItemNames.joinToString(", ")
        val recipeKind = when (type) {
            RecipeType.MEAL -> "full meals (breakfast, lunch, or dinner - something substantial enough to eat as a meal)"
            RecipeType.SNACK -> "quick snacks or light bites (small, fast to prepare, not a full meal)"
        }
        return """
            I have these ingredients at home: $ingredients.

            Suggest 3 simple $recipeKind I can make mostly with these ingredients (I can also use
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
        private const val GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        /** Tried in order after the model chosen in Settings; each has its own free-tier quota. */
        private val FALLBACK_MODELS = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
        private const val ATTEMPTS_PER_MODEL = 2
        private const val DEFAULT_RETRY_DELAY_MS = 3_000L
        private const val MAX_RETRY_DELAY_MS = 10_000L
    }
}
