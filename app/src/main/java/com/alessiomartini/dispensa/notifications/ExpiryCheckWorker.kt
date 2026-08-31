package com.alessiomartini.dispensa.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alessiomartini.dispensa.DispensaApplication
import com.alessiomartini.dispensa.ui.expiry.EXPIRY_WARNING_WINDOW_DAYS
import java.time.LocalDate

class ExpiryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DispensaApplication
        val cutoff = LocalDate.now().plusDays(EXPIRY_WARNING_WINDOW_DAYS)
        val expiringItems = app.itemRepository.findItemsExpiringBy(cutoff)

        if (expiringItems.isNotEmpty()) {
            NotificationHelper.showExpiringItemsNotification(applicationContext, expiringItems)
            app.itemRepository.markNotified(expiringItems)
        }

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "expiry_check_work"
    }
}
