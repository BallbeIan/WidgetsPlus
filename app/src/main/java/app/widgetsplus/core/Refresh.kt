package app.widgetsplus.core

import android.content.Context
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
import java.util.concurrent.TimeUnit

/** Refreshes every module that has at least one widget on the home screen. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Modules.all.filter { it.hasWidgets(applicationContext) }.forEach { module ->
            runCatching { module.refresh(applicationContext) }
        }
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "refresh-periodic"
        private const val NOW = "refresh-now"
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /**
         * Every 30 minutes. Android may delay it when the phone is idle or in battery saver.
         * UPDATE (not KEEP) so work saved by an older version, possibly naming a worker class that no longer exists, is replaced.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES).setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun refreshSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request)
        }

        fun cancelIfNoWidgets(context: Context) {
            if (Modules.placedWidgets(context).isEmpty()) WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
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
        val module = Modules.moduleForReceiver(javaClass) ?: return
        appWidgetIds.forEach { module.forgetWidget(context, it) }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshWorker.cancelIfNoWidgets(context)
    }
}
