package app.widgetsplus.finance.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.widgetsplus.R
import app.widgetsplus.core.RefreshWorker
import app.widgetsplus.core.ui.AppTheme
import app.widgetsplus.core.ui.NameDialog
import app.widgetsplus.core.ui.SectionTitle
import app.widgetsplus.finance.AssetType
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.widget.PriceListWidget
import app.widgetsplus.finance.widget.PriceListWidgetReceiver
import app.widgetsplus.finance.widget.SymbolWidget
import kotlinx.coroutines.launch

/**
 * Chooses what a finance widget shows: a list for the list widget, a symbol for the symbol widget.
 * Opens when the widget is added, from the app's "On your home screen" section, and on Android 12+
 * when the widget is long-pressed and reconfigured.
 */
class FinanceConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isList = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without choosing cancels adding the widget.
        setResult(RESULT_CANCELED, resultIntent())
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        if (info == null) {
            finish()
            return
        }
        isList = info.provider.className == PriceListWidgetReceiver::class.java.name
        val repo = FinanceRepository.get(this)

        setContent {
            AppTheme {
                if (isList) ChooseList(repo) else ChooseSymbol(repo)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ChooseList(repo: FinanceRepository) {
        val s by repo.state.collectAsStateWithLifecycle()
        val current = remember { repo.listForWidget(appWidgetId)?.id }
        var creating by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.choose_list)) }) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(s.lists, key = { it.id }) { list ->
                    Card(Modifier.fillMaxWidth().clickable { done { repo.setListForWidget(appWidgetId, list.id) } }) {
                        Row(Modifier.padding(end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = list.id == current, onClick = { done { repo.setListForWidget(appWidgetId, list.id) } })
                            Column(Modifier.weight(1f)) {
                                Text(list.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (list.items.isEmpty()) stringResource(R.string.empty_list)
                                    else list.items.joinToString(", ") { it.label },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                pluralStringResource(R.plurals.symbols, list.items.size, list.items.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    FilledTonalButton(onClick = { creating = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.new_list))
                    }
                }
                item {
                    Text(
                        stringResource(R.string.choose_list_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (creating) {
            NameDialog(stringResource(R.string.new_list), "", stringResource(R.string.create), onDismiss = { creating = false }) { name ->
                creating = false
                scope.launch {
                    val id = repo.addList(name)
                    done { repo.setListForWidget(appWidgetId, id) }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ChooseSymbol(repo: FinanceRepository) {
        val s by repo.state.collectAsStateWithLifecycle()
        val current = remember { repo.itemForWidget(appWidgetId)?.key }

        Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.choose_symbol)) }) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                s.lists.filter { it.items.isNotEmpty() }.forEach { list ->
                    item(key = "header-${list.id}") { SectionTitle(list.name) }
                    items(list.items, key = { "${list.id}-${it.key}" }) { item ->
                        Card(Modifier.fillMaxWidth().clickable { done { repo.setItemForWidget(appWidgetId, item.key) } }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = item.key == current, onClick = { done { repo.setItemForWidget(appWidgetId, item.key) } })
                                ItemSummary(item, s.quotes[item.key], Modifier.weight(1f).padding(end = 16.dp, top = 8.dp, bottom = 8.dp))
                            }
                        }
                    }
                }
                item {
                    AddSymbolCard(
                        repo = repo,
                        initialType = AssetType.STOCK,
                        // A new symbol goes into the first list that mostly holds its type, else the first list.
                        targetList = { type ->
                            val lists = repo.state.value.lists
                            lists.firstOrNull { it.items.isNotEmpty() && it.mainType == type }?.id
                                ?: lists.firstOrNull()?.id
                                ?: repo.addList(getString(if (type == AssetType.STOCK) R.string.type_stocks else R.string.type_crypto))
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        onAdded = { item -> done { repo.setItemForWidget(appWidgetId, item.key) } },
                    )
                }
            }
        }
    }

    /** Saves the choice, redraws the widget and closes. */
    private fun done(save: () -> Unit) {
        save()
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@FinanceConfigureActivity).getGlanceIdBy(appWidgetId)
            val widget = if (isList) PriceListWidget() else SymbolWidget()
            widget.update(this@FinanceConfigureActivity, glanceId)
            RefreshWorker.schedule(this@FinanceConfigureActivity)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
