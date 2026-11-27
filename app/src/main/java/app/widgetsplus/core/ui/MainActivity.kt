package app.widgetsplus.core.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.widgetsplus.R
import app.widgetsplus.core.Modules
import app.widgetsplus.core.RefreshWorker

/**
 * The app: a tab bar with Widgets (everything on the home screen, and adding more), one tab per category that has
 * something to set up, and Settings. A widget's editor opens on top of the Widgets tab.
 */
class MainActivity : ComponentActivity() {

    /** "widgets", a module id, or "settings". */
    private var tab by mutableStateOf(TAB_WIDGETS)
    /** The widget being edited, or null. */
    private var editing by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.load(this)
        enableEdgeToEdge(AppTheme.barStyle(), AppTheme.barStyle())
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            tab = savedInstanceState.getString(EXTRA_TAB) ?: TAB_WIDGETS
            editing = savedInstanceState.getInt(EXTRA_EDIT, -1).takeIf { it != -1 }
        } else {
            openFrom(intent)
        }
        setContent {
            // Re-apply the bar icon colours when the theme is switched in Settings.
            LaunchedEffect(AppTheme.isDark) { enableEdgeToEdge(AppTheme.barStyle(), AppTheme.barStyle()) }
            WidgetsTheme { App() }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppTheme.load(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFrom(intent)
    }

    /** Widgets open the app on their category's tab. */
    private fun openFrom(intent: Intent?) {
        val module = intent?.getStringExtra(EXTRA_MODULE)?.let { Modules.byId(it) }
        if (module != null) {
            tab = if (module.hasTab) module.id else TAB_WIDGETS
            editing = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_TAB, tab)
        outState.putInt(EXTRA_EDIT, editing ?: -1)
    }

    override fun onStart() {
        super.onStart()
        AppTheme.load(this)
        if (Modules.placedWidgets(this).isNotEmpty()) RefreshWorker.schedule(this)
    }

    @Composable
    private fun App() {
        val tabs = buildList {
            add(NavTab(TAB_WIDGETS, stringResource(R.string.tab_widgets), rememberVectorPainter(Icons.Outlined.Widgets), rememberVectorPainter(Icons.Rounded.Widgets)))
            Modules.all.filter { it.hasTab }.forEach {
                val p = painterResource(it.icon)
                add(NavTab(it.id, stringResource(it.title), p, p))
            }
            add(NavTab(TAB_SETTINGS, stringResource(R.string.tab_settings), rememberVectorPainter(Icons.Outlined.Settings), rememberVectorPainter(Icons.Rounded.Settings)))
        }
        BackHandler(editing != null || tab != TAB_WIDGETS) {
            if (editing != null) editing = null else tab = TAB_WIDGETS
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            bottomBar = { if (editing == null) TabBar(tabs, tab) { tab = it } },
        ) { padding ->
            // consumeWindowInsets: the tab bar's height counts against the keyboard inset, so no gap above the keyboard.
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                val edit = editing
                CompositionLocalProvider(LocalInTabs provides (edit == null)) { when {
                    edit != null -> WidgetEditorScreen(edit, onBack = { editing = null })
                    tab == TAB_WIDGETS -> HomeScreen(onEdit = { editing = it }, onOpenModule = { tab = it })
                    tab == TAB_SETTINGS -> SettingsScreen()
                    else -> Modules.byId(tab)?.Screen() ?: HomeScreen(onEdit = { editing = it }, onOpenModule = { tab = it })
                } }
            }
        }
    }

    companion object {
        const val EXTRA_MODULE = "module"
        private const val EXTRA_TAB = "tab"
        private const val EXTRA_EDIT = "edit"
        const val TAB_WIDGETS = "widgets"
        const val TAB_SETTINGS = "settings"
    }
}

/** The bottom tab bar, as in the other apps: icons over small labels, the selected one in the accent colour. */
@Composable
private fun TabBar(tabs: List<NavTab>, selected: String, onSelect: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f)) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 7.dp, bottom = 3.dp), contentAlignment = Alignment.Center) {
                Row(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    tabs.forEach { t ->
                        val on = selected == t.key
                        val tint = if (on) Accent else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(
                            Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSelect(t.key) }.padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(if (on) t.selectedIcon else t.icon, t.label, tint = tint, modifier = Modifier.size(26.dp))
                            Text(t.label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private data class NavTab(val key: String, val label: String, val icon: Painter, val selectedIcon: Painter)
