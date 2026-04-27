package com.safistep.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.safistep.data.repository.BlacklistRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// ── Boot Receiver ─────────────────────────────────────────────
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scheduleSyncWork(context)
        }
    }
}

fun scheduleSyncWork(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // Periodic sync every 6 hours
    val periodicSync = PeriodicWorkRequestBuilder<BlacklistSyncWorker>(6, TimeUnit.HOURS)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "blacklist_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        periodicSync
    )

    // Immediate sync on boot
    val immediateSync = OneTimeWorkRequestBuilder<BlacklistSyncWorker>()
        .setConstraints(constraints)
        .build()
    WorkManager.getInstance(context).enqueue(immediateSync)

    // Sync unsubmitted reports
    val reportSync = OneTimeWorkRequestBuilder<ReportSyncWorker>()
        .setConstraints(constraints)
        .build()
    WorkManager.getInstance(context).enqueue(reportSync)
}

// ── Blacklist Sync Worker ─────────────────────────────────────
@HiltWorker
class BlacklistSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val blacklistRepo: BlacklistRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            blacklistRepo.syncIfNeeded()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// ── Report Sync Worker ────────────────────────────────────────
@HiltWorker
class ReportSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val reportRepo: com.safistep.data.repository.ReportRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            reportRepo.syncPendingReports()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
