package com.alessiomartini.dispensa

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alessiomartini.dispensa.data.AppDatabase
import com.alessiomartini.dispensa.data.ItemRepository
import com.alessiomartini.dispensa.network.RecipeSuggestionRepository
import com.alessiomartini.dispensa.network.SyncRepository
import com.alessiomartini.dispensa.network.UpdateRepository
import com.alessiomartini.dispensa.notifications.ExpiryCheckWorker
import com.alessiomartini.dispensa.notifications.NotificationHelper
import com.alessiomartini.dispensa.settings.SettingsRepository
import com.alessiomartini.dispensa.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DispensaApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val itemRepository: ItemRepository by lazy {
        ItemRepository(database.itemDao(), database.purchaseHistoryDao())
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val recipeSuggestionRepository: RecipeSuggestionRepository by lazy {
        RecipeSuggestionRepository(settingsRepository)
    }
    val updateRepository: UpdateRepository by lazy { UpdateRepository(this) }
    val syncRepository: SyncRepository by lazy {
        SyncRepository(database.itemDao(), database.purchaseHistoryDao(), settingsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        scheduleExpiryChecks()
        scheduleSync()
        // Cheap (only items still under "Other"), and picks up catalog additions shipped in updates.
        applicationScope.launch { itemRepository.recategorizeUncategorized() }
    }

    private fun scheduleExpiryChecks() {
        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ExpiryCheckWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
