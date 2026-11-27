package app.widgetsplus.days

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.Hue
import app.widgetsplus.core.WidgetKind
import app.widgetsplus.core.WidgetModule
import app.widgetsplus.days.ui.CountdownConfigureActivity
import app.widgetsplus.days.ui.DaysScreen
import app.widgetsplus.days.widget.CountdownWidget
import app.widgetsplus.days.widget.CountdownWidgetReceiver
import app.widgetsplus.days.widget.ProgressWidget
import app.widgetsplus.days.widget.ProgressWidgetReceiver

/** Countdowns to the user's events, and how far through the day, week, month and year we are. Nothing is fetched. */
object DaysModule : WidgetModule {
    override val id = "days"
    override val title = R.string.days_title
    override val description = R.string.days_description
    override val icon = R.drawable.ic_module_days
    override val hue = Hue.PINK

    val countdownWidget = WidgetKind(CountdownWidgetReceiver::class.java, R.string.days_widget_countdown_name, R.string.days_widget_countdown_description, R.drawable.ic_widget_countdown, CountdownConfigureActivity::class.java)
    val progressWidget = WidgetKind(ProgressWidgetReceiver::class.java, R.string.days_widget_progress_name, R.string.days_widget_progress_description, R.drawable.ic_widget_progress, null)
    override val widgets = listOf(countdownWidget, progressWidget)

    @Composable
    override fun summary(): String {
        val s by DaysRepository.get(LocalContext.current).state.collectAsStateWithLifecycle()
        val n = s.events.size
        return if (n == 0) stringResource(R.string.days_summary_empty) else pluralStringResource(R.plurals.days_events, n, n)
    }

    @Composable
    override fun Screen() = DaysScreen()

    /** Nothing to fetch: redrawing keeps the day counts and progress bars current. */
    override suspend fun refresh(context: Context): Boolean {
        updateWidgets(context)
        return true
    }

    override suspend fun updateWidgets(context: Context) {
        // Running widgets only recompose when their state changes, so bump it: "today" may have moved on.
        DaysRepository.get(context).touch()
        CountdownWidget().updateAll(context)
        ProgressWidget().updateAll(context)
    }

    override fun describeWidget(context: Context, appWidgetId: Int, kind: WidgetKind): String = when (kind) {
        countdownWidget -> DaysRepository.get(context).eventForWidget(appWidgetId)?.name ?: context.getString(R.string.nothing_chosen)
        else -> context.getString(R.string.days_progress_describe)
    }

    override fun forgetWidget(context: Context, appWidgetId: Int) = DaysRepository.get(context).forgetWidget(appWidgetId)
}
