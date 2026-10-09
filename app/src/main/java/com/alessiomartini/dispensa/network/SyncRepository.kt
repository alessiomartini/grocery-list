package com.alessiomartini.dispensa.network

import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemDao
import com.alessiomartini.dispensa.data.ItemStatus
import com.alessiomartini.dispensa.data.PurchaseHistoryDao
import com.alessiomartini.dispensa.data.PurchaseRecord
import com.alessiomartini.dispensa.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Wire format of backend/src/worker.js: instants as epoch millis, dates as epoch days. */
@Serializable
data class SyncItemDto(
    val uuid: String,
    val name: String,
    val quantity: Int,
    val unit: String,
    val category: String,
    val status: String,
    val expiryEpochDay: Long? = null,
    val addedAt: Long,
    val statusChangedAt: Long,
    val expiryNotified: Boolean,
    val deleted: Boolean,
    val updatedAt: Long
)

@Serializable
data class SyncPurchaseDto(
    val uuid: String,
    val name: String,
    val category: String,
    val purchasedAt: Long,
    val updatedAt: Long
)

@Serializable
private data class PushItemsRequest(val rows: List<SyncItemDto>)

@Serializable
private data class PushPurchasesRequest(val rows: List<SyncPurchaseDto>)

@Serializable
private data class PushResponse(val ok: Boolean = false, val error: String? = null)

@Serializable
private data class PullItemsResponse(val serverTime: Long, val hasMore: Boolean, val rows: List<SyncItemDto> = emptyList())

@Serializable
private data class PullPurchasesResponse(val serverTime: Long, val hasMore: Boolean, val rows: List<SyncPurchaseDto> = emptyList())

@Serializable
private data class ErrorResponse(val error: String? = null)

sealed interface SyncResult {
    data class Success(val itemsSynced: Int, val purchasesSynced: Int) : SyncResult
    data object NotConfigured : SyncResult
    data class Error(val message: String) : SyncResult
}

private fun GroceryItem.toDto() = SyncItemDto(
    uuid = uuid,
    name = name,
    quantity = quantity,
    unit = unit,
    category = category,
    status = status.name,
    expiryEpochDay = expiryDate?.toEpochDay(),
    addedAt = addedAt.toEpochMilli(),
    statusChangedAt = statusChangedAt.toEpochMilli(),
    expiryNotified = expiryNotified,
    deleted = deleted,
    updatedAt = updatedAt.toEpochMilli()
)

private fun SyncItemDto.toEntity(localId: Long) = GroceryItem(
    id = localId,
    uuid = uuid,
    name = name,
    quantity = quantity,
    unit = unit,
    category = category,
    status = ItemStatus.valueOf(status),
    expiryDate = expiryEpochDay?.let { LocalDate.ofEpochDay(it) },
    addedAt = Instant.ofEpochMilli(addedAt),
    statusChangedAt = Instant.ofEpochMilli(statusChangedAt),
    expiryNotified = expiryNotified,
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deleted = deleted
)

private fun PurchaseRecord.toDto() = SyncPurchaseDto(
    uuid = uuid,
    name = name,
    category = category,
    purchasedAt = purchasedAt.toEpochDay(),
    updatedAt = updatedAt.toEpochMilli()
)

private fun SyncPurchaseDto.toEntity() = PurchaseRecord(
    uuid = uuid,
    name = name,
    category = category,
    purchasedAt = LocalDate.ofEpochDay(purchasedAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

/**
 * Mirrors the pantry to the pantry-api Worker (see /backend), for backup and so it can be edited
 * from outside the app. The phone stays the primary copy.
 *
 * Rows are matched by uuid, never by the local autoincrement id, and the server keeps whichever
 * write has the newer updatedAt - so pushing the same row twice is harmless.
 *
 * There's deliberately no push watermark: every sync pushes every local row, deleted ones
 * included. A personal pantry stays in the hundreds-to-low-thousands of rows even after years,
 * and it removes the whole class of bugs where a partially failed push leaves a watermark past
 * rows the server never got. Pulls are watermarked, since the server can hold more than the
 * device has seen.
 */
class SyncRepository(
    private val itemDao: ItemDao,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val settingsRepository: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private class SyncException(message: String) : Exception(message)

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        if (!settings.syncConfigured) return@withContext SyncResult.NotConfigured

        try {
            // Push first, so edits made just before tapping "Sync now" are on the server
            // before anything else asks it what's new.
            val itemsPushed = pushItems(settings.syncUrl, settings.syncToken)
            val purchasesPushed = pushPurchases(settings.syncUrl, settings.syncToken)
            val itemsPulled = pullItems(settings.syncUrl, settings.syncToken, settings.itemsSyncedThrough)
            val purchasesPulled = pullPurchases(settings.syncUrl, settings.syncToken, settings.purchasesSyncedThrough)
            SyncResult.Success(
                itemsSynced = itemsPushed + itemsPulled,
                purchasesSynced = purchasesPushed + purchasesPulled
            )
        } catch (e: SyncException) {
            SyncResult.Error(e.message ?: "Sync failed")
        } catch (e: IOException) {
            SyncResult.Error(e.message ?: "Network error")
        } catch (e: IllegalArgumentException) {
            // Malformed URL in settings, or a response that isn't the JSON we expect.
            SyncResult.Error(e.message ?: "Unexpected response")
        } finally {
            settingsRepository.setLastSyncAt(System.currentTimeMillis())
        }
    }

    private fun authedRequest(url: String, token: String) = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $token")

    private fun errorMessage(code: Int, body: String): String =
        runCatching { json.decodeFromString(ErrorResponse.serializer(), body).error }.getOrNull()
            ?: "HTTP $code"

    private fun post(url: String, token: String, body: String) {
        val request = authedRequest(url, token).post(body.toRequestBody(jsonMediaType)).build()
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw SyncException(errorMessage(response.code, bodyString))
            val parsed = runCatching { json.decodeFromString(PushResponse.serializer(), bodyString) }.getOrNull()
            if (parsed?.ok != true) throw SyncException(parsed?.error ?: "Push rejected by the server")
        }
    }

    private fun get(url: String, token: String): String {
        val request = authedRequest(url, token).get().build()
        return client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw SyncException(errorMessage(response.code, bodyString))
            bodyString
        }
    }

    private suspend fun pushItems(baseUrl: String, token: String): Int {
        val rows = itemDao.findUpdatedSince(0).map { it.toDto() }
        rows.chunked(MAX_ROWS_PER_REQUEST).forEach { batch ->
            post("$baseUrl/api/items", token, json.encodeToString(PushItemsRequest.serializer(), PushItemsRequest(batch)))
        }
        return rows.size
    }

    private suspend fun pushPurchases(baseUrl: String, token: String): Int {
        val rows = purchaseHistoryDao.findUpdatedSince(0).map { it.toDto() }
        rows.chunked(MAX_ROWS_PER_REQUEST).forEach { batch ->
            post("$baseUrl/api/purchases", token, json.encodeToString(PushPurchasesRequest.serializer(), PushPurchasesRequest(batch)))
        }
        return rows.size
    }

    private suspend fun pullItems(baseUrl: String, token: String, since: Long): Int {
        var cursor = since
        var total = 0
        while (true) {
            val page = json.decodeFromString(PullItemsResponse.serializer(), get("$baseUrl/api/items?since=$cursor", token))
            for (dto in page.rows) {
                val existing = itemDao.findByUuid(dto.uuid)
                when {
                    existing == null -> itemDao.upsert(dto.toEntity(localId = 0))
                    dto.updatedAt > existing.updatedAt.toEpochMilli() -> itemDao.update(dto.toEntity(localId = existing.id))
                    // Otherwise the local copy is at least as new - typically our own push coming back.
                }
            }
            total += page.rows.size
            settingsRepository.setItemsSyncedThrough(page.serverTime)
            if (!page.hasMore) break
            cursor = page.rows.lastOrNull()?.updatedAt ?: break
        }
        return total
    }

    private suspend fun pullPurchases(baseUrl: String, token: String, since: Long): Int {
        var cursor = since
        var total = 0
        while (true) {
            val page = json.decodeFromString(PullPurchasesResponse.serializer(), get("$baseUrl/api/purchases?since=$cursor", token))
            for (dto in page.rows) {
                // Purchase records never change after creation: insert if unknown, nothing to merge.
                if (purchaseHistoryDao.findByUuid(dto.uuid) == null) {
                    purchaseHistoryDao.insert(dto.toEntity())
                }
            }
            total += page.rows.size
            settingsRepository.setPurchasesSyncedThrough(page.serverTime)
            if (!page.hasMore) break
            cursor = page.rows.lastOrNull()?.updatedAt ?: break
        }
        return total
    }

    companion object {
        // Same as MAX_ROWS_PER_REQUEST in backend/src/worker.js.
        private const val MAX_ROWS_PER_REQUEST = 500
    }
}
