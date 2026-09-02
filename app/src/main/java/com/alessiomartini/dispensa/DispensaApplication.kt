package com.alessiomartini.dispensa

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alessiomartini.dispensa.data.AppDatabase
import com.alessiomartini.dispensa.data.ItemRepository
import com.alessiomartini.dispensa.network.RecipeSuggestionRepository
import com.alessiomartini.dispensa.network.UpdateRepository
import com.alessiomartini.dispensa.notifications.ExpiryCheckWorker
import com.alessiomartini.dispensa.notifications.NotificationHelper
import com.alessiomartini.dispensa.settings.SettingsRepository
import java.util.concurrent.TimeUnit

class DispensaApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val itemRepository: ItemRepository by lazy { ItemRepository(database.itemDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val recipeSuggestionRepository: RecipeSuggestionRepository by lazy {
        RecipeSuggestionRepository(settingsRepository)
    }
    val updateRepository: UpdateRepository by lazy { UpdateRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        scheduleExpiryChecks()
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
}
