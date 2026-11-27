package app.widgetsplus.core.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import app.widgetsplus.core.RefreshWorker
import kotlinx.coroutines.launch

/**
 * Base for every module's "choose what this widget shows" screen. It opens when a widget is added, from the
 * app's "On your home screen" section, and on Android 12+ when the widget is long-pressed and reconfigured.
 * Backing out without choosing cancels adding the widget.
 */
abstract class WidgetConfigureActivity : ComponentActivity() {

    protected var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    /** The class name of the widget's receiver, for modules whose widgets share one configure screen. */
    protected var receiverClassName = ""
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.load(this)
        enableEdgeToEdge(AppTheme.barStyle(), AppTheme.barStyle())
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, resultIntent())
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        if (info == null) {
            finish()
            return
        }
        receiverClassName = info.provider.className
        setContent { WidgetsTheme { Content() } }
    }

    @Composable
    protected abstract fun Content()

    /** Saves the choice, redraws this widget with [widget], and closes. */
    protected fun finishWith(widget: GlanceAppWidget, save: () -> Unit) {
        save()
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigureActivity).getGlanceIdBy(appWidgetId)
            widget.update(this@WidgetConfigureActivity, glanceId)
            RefreshWorker.schedule(this@WidgetConfigureActivity)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

/** The page every configure screen uses: large title, then grouped content. */
@Composable
fun ConfigureScaffold(title: String, content: LazyListScope.() -> Unit) = Page(title, content = content)

/**
 * One option on a configure screen, as its own grouped row with a check mark when it is the current choice.
 * For several options in one group, use [CheckRow] inside a [Grouped] instead.
 */
@Composable
fun ChoiceCard(selected: Boolean, title: String, subtitle: String?, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Grouped(Modifier.padding(top = 8.dp)) {
        ListRow(title, subtitle = subtitle, onClick = onClick, trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailing()
                if (selected) Icon(Icons.Rounded.Check, null, tint = Accent, modifier = Modifier.padding(start = 8.dp).size(22.dp))
            }
        })
    }
}
