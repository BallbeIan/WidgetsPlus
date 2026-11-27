package app.widgetsplus.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.widgetsplus.R
import app.widgetsplus.core.Modules

/**
 * The Widgets tab: every WidgetsPlus widget on the home screen (tap one to change what it shows and how it looks),
 * then every widget there is to add, by category.
 */
@Composable
fun HomeScreen(onEdit: (Int) -> Unit, onOpenModule: (String) -> Unit) {
    val context = LocalContext.current
    // Widgets can be added or removed while the app is in the background, so re-read them on every resume.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val placed = remember(resumes) { Modules.placedWidgets(context) }

    Page(stringResource(R.string.tab_widgets)) {
        item { SectionHeader(stringResource(R.string.on_home_screen)) }
        if (placed.isEmpty()) {
            item {
                Grouped {
                    Text(
                        stringResource(R.string.no_widgets_yet),
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                Grouped {
                    placed.forEachIndexed { i, w ->
                        if (i > 0) Hairline(IconRowIndent)
                        val description = remember(w, resumes) { w.module.describeWidget(context, w.appWidgetId, w.kind) }
                        ListRow(
                            title = stringResource(w.kind.name),
                            subtitle = description,
                            onClick = { onEdit(w.appWidgetId) },
                            leading = { Squircle(w.kind.icon, w.module.hue.color) },
                            trailing = { Chevron() },
                        )
                    }
                }
            }
            item { Footnote(stringResource(R.string.home_edit_help)) }
        }

        Modules.all.forEach { module ->
            item(key = "add-${module.id}") {
                SectionHeader(stringResource(module.title)) {
                    if (module.hasTab) {
                        Text(
                            stringResource(R.string.set_up),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Accent,
                            modifier = Modifier.padding(start = 8.dp).clickableNoRipple { onOpenModule(module.id) },
                        )
                    }
                }
            }
            item(key = "kinds-${module.id}") { ModuleWidgetsCard(module) }
        }
    }
}
