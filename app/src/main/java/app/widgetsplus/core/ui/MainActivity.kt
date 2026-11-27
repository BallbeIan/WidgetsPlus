package app.widgetsplus.core.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.widgetsplus.R
import app.widgetsplus.core.Modules
import app.widgetsplus.core.PlacedWidget
import app.widgetsplus.core.RefreshWorker
import app.widgetsplus.core.WidgetModule

/** The hub: widget categories and the widgets on the home screen. Each category opens its own screen. */
class MainActivity : ComponentActivity() {

    /** The open module screen, or null for the home screen. */
    private var openModule by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openModule = if (savedInstanceState != null) savedInstanceState.getString(EXTRA_MODULE) else intent.getStringExtra(EXTRA_MODULE)
        setContent {
            AppTheme {
                val module = openModule?.let { Modules.byId(it) }
                if (module == null) {
                    HomeScreen(onOpen = { openModule = it.id })
                } else {
                    BackHandler { openModule = null }
                    module.Screen(onBack = { openModule = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_MODULE)?.let { openModule = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_MODULE, openModule)
    }

    override fun onStart() {
        super.onStart()
        // Each module screen refreshes its own data when opened; this only keeps the background refresh alive.
        if (Modules.placedWidgets(this).isNotEmpty()) RefreshWorker.schedule(this)
    }

    companion object {
        const val EXTRA_MODULE = "module"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(onOpen: (WidgetModule) -> Unit) {
    val context = LocalContext.current
    // Widgets can be added or removed while the app is in the background, so re-read them on every resume.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val placed = remember(resumes) { Modules.placedWidgets(context) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle(stringResource(R.string.categories)) }
            items(Modules.all, key = { it.id }) { module -> ModuleCard(module) { onOpen(module) } }

            item { SectionTitle(stringResource(R.string.on_home_screen)) }
            item { PlacedWidgetsCard(placed, resumes) }
        }
    }
}

@Composable
private fun ModuleCard(module: WidgetModule, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(module.icon, size = 48)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(module.title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(module.description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(module.summary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Every widget on the home screen and what it shows. Tapping one opens its settings. */
@Composable
private fun PlacedWidgetsCard(placed: List<PlacedWidget>, refreshKey: Int) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        if (placed.isEmpty()) {
            Text(stringResource(R.string.no_widgets_yet), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            return@Card
        }
        placed.forEachIndexed { i, w ->
            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            val description = remember(w, refreshKey) { w.module.describeWidget(context, w.appWidgetId, w.kind) }
            val configure = w.kind.configure
            var row = Modifier.fillMaxWidth()
            if (configure != null) {
                row = row.clickable {
                    context.startActivity(
                        Intent(context, configure).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, w.appWidgetId),
                    )
                }
            }
            Row(row.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconTile(w.kind.icon)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.widget_title_format, stringResource(w.module.title), stringResource(w.kind.name)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (configure != null) Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.change))
            }
        }
    }
}
