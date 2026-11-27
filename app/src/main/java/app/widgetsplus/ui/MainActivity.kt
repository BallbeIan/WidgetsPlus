package app.widgetsplus.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.widgetsplus.R
import app.widgetsplus.data.Format
import app.widgetsplus.data.Repository
import app.widgetsplus.widget.RefreshWorker
import app.widgetsplus.widget.SingleQuoteWidgetReceiver
import app.widgetsplus.widget.WatchlistWidgetReceiver
import app.widgetsplus.widget.hasAnyWidgets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repo = Repository.get(this)
        setContent { AppTheme { MainScreen(repo) } }
    }

    override fun onStart() {
        super.onStart()
        val repo = Repository.get(this)
        if (hasAnyWidgets(this)) RefreshWorker.schedule(this)
        // Fresh prices on opening, unless they were fetched in the last few minutes.
        if (System.currentTimeMillis() - repo.state.value.lastRefresh > 5 * 60_000) {
            lifecycleScope.launch { repo.refresh() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(repo: Repository) {
    val s by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (s.refreshing) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        IconButton(onClick = { scope.launch { repo.refresh() } }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    when {
                        s.lastRefresh == 0L -> stringResource(R.string.not_updated_yet)
                        s.lastRefreshFailed -> stringResource(R.string.offline_since, Format.time(context, s.lastRefresh))
                        else -> stringResource(R.string.updated_at, Format.time(context, s.lastRefresh))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.lastRefreshFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    if (s.items.isEmpty()) {
                        Text(stringResource(R.string.list_empty), Modifier.padding(16.dp))
                    }
                    s.items.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider()
                        ItemSummary(
                            item = item,
                            quote = s.quotes[item.key],
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        ) {
                            Column {
                                IconButton(onClick = { scope.launch { repo.move(item.key, -1) } }, enabled = i > 0, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                                }
                                IconButton(onClick = { scope.launch { repo.move(item.key, 1) } }, enabled = i < s.items.lastIndex, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                                }
                            }
                            IconButton(onClick = { scope.launch { repo.remove(item.key) } }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove, item.label))
                            }
                        }
                    }
                }
            }
            item { AddSymbolCard(repo, Modifier.fillMaxWidth()) }
            item { AddWidgetCard() }
            item {
                Text(
                    stringResource(R.string.sources_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddWidgetCard() {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.widgets_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.widgets_help), style = MaterialTheme.typography.bodyMedium)
            // Some launchers let an app offer to place a widget directly.
            if (manager.isRequestPinAppWidgetSupported) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        manager.requestPinAppWidget(ComponentName(context, SingleQuoteWidgetReceiver::class.java), null, null)
                    }) { Text(stringResource(R.string.widget_single_name)) }
                    FilledTonalButton(onClick = {
                        manager.requestPinAppWidget(ComponentName(context, WatchlistWidgetReceiver::class.java), null, null)
                    }) { Text(stringResource(R.string.watchlist)) }
                }
            }
        }
    }
}
