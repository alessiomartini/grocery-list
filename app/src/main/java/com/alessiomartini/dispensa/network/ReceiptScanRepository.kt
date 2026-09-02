package com.alessiomartini.dispensa.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.alessiomartini.dispensa.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class ScannedItem(val name: String, val quantity: Int = 1)

sealed interface ReceiptScanResult {
    data class Success(val items: List<ScannedItem>) : ReceiptScanResult
    data object NoApiKey : ReceiptScanResult
    data class Error(val message: String) : ReceiptScanResult
}

/**
 * Sends a photo of a grocery receipt to Gemini (same key/model as recipe suggestions - the
 * default model is multimodal) and asks it to list the purchased items as JSON.
 */
class ReceiptScanRepository(
    private val settingsRepository: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun scanReceipt(photoFile: File): ReceiptScanResult = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        if (settings.apiKey.isBlank()) return@withContext ReceiptScanResult.NoApiKey

        val base64Image = try {
            downscaleAndEncode(photoFile)
        } catch (e: Exception) {
            return@withContext ReceiptScanResult.Error(e.message ?: "Couldn't read the photo")
        }

        val requestBody = json.encodeToString(
            GeminiRequest.serializer(),
            GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = PROMPT),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    maxOutputTokens = 1000,
                    responseMimeType = "application/json"
                )
            )
        )

        val request = Request.Builder()
            .url("$GEMINI_API_BASE_URL/${settings.model}:generateContent")
            .addHeader("x-goog-api-key", settings.apiKey)
            .addHeader("content-type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        json.decodeFromString(GeminiResponse.serializer(), bodyString).error?.message
                    }.getOrNull() ?: "HTTP ${response.code}"
                    return@withContext ReceiptScanResult.Error(message)
                }

                val parsed = json.decodeFromString(GeminiResponse.serializer(), bodyString)
                val text = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                    ?: return@withContext ReceiptScanResult.Error("Empty response from the model")

                val items = parseItems(text)
                if (items.isEmpty()) {
                    ReceiptScanResult.Error("Couldn't find any items on that receipt")
                } else {
                    ReceiptScanResult.Success(items)
                }
            }
        } catch (e: IOException) {
            ReceiptScanResult.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            ReceiptScanResult.Error(e.message ?: "Unexpected error")
        }
    }

    private fun parseItems(rawText: String): List<ScannedItem> {
        val cleaned = rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(ScannedItem.serializer()),
            cleaned
        )
    }

    private fun downscaleAndEncode(file: File): String {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, boundsOptions)

        var sampleSize = 1
        val maxDimension = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
        while (maxDimension / sampleSize > MAX_IMAGE_DIMENSION) sampleSize *= 2

        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: throw IOException("Couldn't decode the photo")

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        bitmap.recycle()
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MAX_IMAGE_DIMENSION = 1600

        private val PROMPT = """
            This image is a grocery store receipt. Extract every distinct grocery/food or
            household item purchased (ignore prices, totals, taxes, store name, and non-item
            lines). Reply with ONLY a valid JSON array (no text before or after, no markdown),
            where each element has:
            - "name": a short, generic product name in English (e.g. "Milk", not the exact
              receipt abbreviation)
            - "quantity": how many units were purchased (integer, default 1 if unclear)

            If the image isn't a receipt or no items can be read, reply with an empty array [].
        """.trimIndent()
    }
}
