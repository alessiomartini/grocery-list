package com.alessiomartini.dispensa.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alessiomartini.dispensa.DispensaApplication
import com.alessiomartini.dispensa.network.SyncResult

/** Scheduled hourly; does nothing until a server URL and token are saved in Settings. */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DispensaApplication
        return when (app.syncRepository.syncNow()) {
            is SyncResult.Success, SyncResult.NotConfigured -> Result.success()
            // Usually no network or a server blip: let WorkManager back off and try again.
            is SyncResult.Error -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "sync_work"
    }
}
