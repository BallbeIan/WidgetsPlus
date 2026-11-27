package app.widgetsplus.core.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
        enableEdgeToEdge()
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
        setContent { AppTheme { Content() } }
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

/** The scrolling page every configure screen uses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureScaffold(title: String, content: LazyListScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** A card with a radio button, a title and an optional second line. */
@Composable
fun ChoiceCard(selected: Boolean, title: String, subtitle: String?, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            trailing()
        }
    }
}
