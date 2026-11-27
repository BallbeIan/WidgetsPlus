package app.widgetsplus.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.widgetsplus.R
import app.widgetsplus.data.Repository
import app.widgetsplus.data.WatchItem
import app.widgetsplus.widget.RefreshWorker
import app.widgetsplus.widget.SingleQuoteWidget
import kotlinx.coroutines.launch

/** Opens when a single-symbol widget is added (and on Android 12+ when it is long-pressed and reconfigured). */
class ConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without choosing cancels adding the widget.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val repo = Repository.get(this)
        val current = repo.itemForWidget(appWidgetId)?.key

        setContent {
            AppTheme {
                val s by repo.state.collectAsStateWithLifecycle()
                Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.choose_symbol)) }) }) { padding ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().imePadding(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.items, key = { it.key }) { item ->
                            Card(Modifier.fillMaxWidth().clickable { choose(repo, item) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = item.key == current, onClick = { choose(repo, item) })
                                    ItemSummary(
                                        item,
                                        s.quotes[item.key],
                                        Modifier.weight(1f).padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                                    )
                                }
                            }
                        }
                        item {
                            AddSymbolCard(repo, Modifier.fillMaxWidth().padding(top = 8.dp), onAdded = { choose(repo, it) })
                        }
                        item {
                            Text(
                                stringResource(R.string.choose_symbol_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun choose(repo: Repository, item: WatchItem) {
        repo.setItemForWidget(appWidgetId, item.key)
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@ConfigureActivity).getGlanceIdBy(appWidgetId)
            SingleQuoteWidget().update(this@ConfigureActivity, glanceId)
            RefreshWorker.schedule(this@ConfigureActivity)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
