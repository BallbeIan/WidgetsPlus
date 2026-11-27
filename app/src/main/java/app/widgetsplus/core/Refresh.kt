package app.widgetsplus.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Refreshes every module that has at least one widget on the home screen. It runs without a network connection
 * too: modules that fetch data then fail fast and show "Offline", while the others (Days) still redraw.
 */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Modules.all.filter { it.hasWidgets(applicationContext) }.forEach { module ->
            runCatching { module.refresh(applicationContext) }
        }
        RedrawWorker.scheduleMidnight(applicationContext, replace = false)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "refresh-periodic"
        private const val NOW = "refresh-now"
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /**
         * Every 30 minutes, with or without a connection. Android may delay it when the phone is idle or in battery saver.
         * UPDATE (not KEEP) so work saved by an older version, possibly naming a worker class that no longer exists, is replaced.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
            RedrawWorker.scheduleMidnight(context, replace = false)
        }

        /** For the refresh buttons: waits for a connection, since the user asked for fresh data. */
        fun refreshSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request)
        }

        fun cancelIfNoWidgets(context: Context) {
            if (Modules.placedWidgets(context).isNotEmpty()) return
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
            RedrawWorker.cancel(context)
        }
    }
}

/**
 * Redraws all widgets from stored data, without fetching anything. Runs just after local midnight so day counts,
 * "Today" and progress bars move on, and when the time or time zone is changed.
 */
class RedrawWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Modules.all.filter { it.hasWidgets(applicationContext) }.forEach { module ->
            runCatching { module.updateWidgets(applicationContext) }
        }
        return Result.success()
    }

    companion object {
        private const val MIDNIGHT = "redraw-midnight"
        private const val NOW = "redraw-now"

        /**
         * Schedules the next run a minute after local midnight. [replace] is for when the clock changed; otherwise
         * an already scheduled run is kept. The periodic refresh calls this, so a new one follows each midnight.
         */
        fun scheduleMidnight(context: Context, replace: Boolean) {
            val now = ZonedDateTime.now()
            val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
            val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<RedrawWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                MIDNIGHT,
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun redrawNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RedrawWorker>().build())
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(MIDNIGHT)
    }
}

/** The time or time zone was changed (also when travelling): redraw now, and move the midnight redraw. */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED) return
        if (Modules.placedWidgets(context).isEmpty()) return
        RedrawWorker.redrawNow(context)
        RedrawWorker.scheduleMidnight(context, replace = true)
    }
}

/** Base for every widget receiver: keeps the background refresh running while widgets exist. */
abstract class ModuleWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshWorker.schedule(context)
        RefreshWorker.refreshSoon(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetStyles.forget(context, it) }
        val module = Modules.moduleForReceiver(javaClass) ?: return
        appWidgetIds.forEach { module.forgetWidget(context, it) }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshWorker.cancelIfNoWidgets(context)
    }
}
