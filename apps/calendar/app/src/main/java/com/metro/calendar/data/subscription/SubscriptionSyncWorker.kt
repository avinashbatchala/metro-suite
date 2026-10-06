package com.metro.calendar.data.subscription

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.metro.calendar.tiles.CalendarTileRefresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Refreshes every enabled ICS subscription in the background. A failed refresh keeps the last-good
 * cache, so we schedule a retry rather than surfacing an error here.
 */
class SubscriptionSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val source = IcsCalendarSource(CalendarSubscriptionStore(applicationContext))
        val failures = withContext(Dispatchers.IO) { source.syncAll() }
        CalendarTileRefresh.request(applicationContext)
        return if (failures == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC_WORK = "calendar-subscription-sync"
        private const val ONESHOT_WORK = "calendar-subscription-sync-once"

        private val networkOnly = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SubscriptionSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(networkOnly)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SubscriptionSyncWorker>()
                .setConstraints(networkOnly)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                ONESHOT_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}
