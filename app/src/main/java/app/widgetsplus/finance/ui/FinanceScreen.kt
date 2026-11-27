package app.widgetsplus.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.core.ui.MoveButtons
import app.widgetsplus.core.ui.RemoveButton
import app.widgetsplus.core.ui.EditToggle
import app.widgetsplus.core.ui.Chip
import app.widgetsplus.R
import app.widgetsplus.core.ui.Accent
import app.widgetsplus.core.ui.ActionRow
import app.widgetsplus.core.ui.ConfirmDialog
import app.widgetsplus.core.ui.Footnote
import app.widgetsplus.core.ui.Grouped
import app.widgetsplus.core.ui.Hairline
import app.widgetsplus.core.ui.ModuleWidgetsCard
import app.widgetsplus.core.ui.NameDialog
import app.widgetsplus.core.ui.Page
import app.widgetsplus.core.ui.SectionHeader
import app.widgetsplus.core.ui.clickableNoRipple
import app.widgetsplus.core.widget.formatTime
import app.widgetsplus.finance.FinanceModule
import app.widgetsplus.finance.FinanceRepository
import kotlinx.coroutines.launch

/**
 * The Finance tab: a row of list chips, the selected list's symbols, adding a symbol, and the finance widgets.
 * "Edit" next to the list's name shows remove and reorder buttons on each symbol, and the list's own actions.
 */
@Composable
fun FinanceScreen() {
    val context = LocalContext.current
    val repo = remember { FinanceRepository.get(context) }
    val s by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedIndex = s.lists.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    val list = s.lists.getOrNull(selectedIndex)

    // Fresh prices on opening, unless they were fetched in the last few minutes.
    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - repo.state.value.lastRefresh > 5 * 60_000) repo.refresh()
    }

    var editing by rememberSaveable { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Page(
        title = stringResource(R.string.finance_title),
        actions = {
            if (s.refreshing) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Accent, strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = { scope.launch { repo.refresh() } }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh), tint = Accent)
                }
            }
        },
    ) {
        // The lists, as capsule chips, with "+" for a new one.
        item(key = "chips") {
            LazyRow(
                Modifier.padding(top = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(s.lists, key = { it.id }) { l ->
                    Chip(l.name, selected = l.id == list?.id, onClick = { selectedId = l.id })
                }
                item(key = "new") {
                    Surface(onClick = { creating = true }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.new_list),
                            tint = Accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(22.dp),
                        )
                    }
                }
            }
        }

        if (list == null) {
            item { SectionHeader("") }
            item { Grouped { ActionRow(stringResource(R.string.new_list), onClick = { creating = true }) } }
            item { Footnote(stringResource(R.string.no_lists)) }
        } else {
            item(key = "list-header") {
                SectionHeader(list.name) {
                    EditToggle(editing) { editing = !editing }
                }
            }
            item(key = "list-items") {
                Grouped {
                    if (list.items.isEmpty()) {
                        Text(
                            stringResource(R.string.finance_list_empty_app),
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    list.items.forEachIndexed { i, item ->
                        if (i > 0) Hairline(if (editing) 60.dp else 16.dp)
                        ItemSummary(
                            item = item,
                            quote = s.quotes[item.key],
                            showQuote = !editing,
                            leading = if (editing) ({
                                RemoveButton(stringResource(R.string.remove, item.label)) {
                                    scope.launch { repo.removeItem(list.id, item.key) }
                                }
                            }) else null,
                            trailing = if (editing) ({
                                MoveButtons(
                                    canUp = i > 0,
                                    canDown = i < list.items.lastIndex,
                                    onUp = { scope.launch { repo.moveItem(list.id, item.key, -1) } },
                                    onDown = { scope.launch { repo.moveItem(list.id, item.key, 1) } },
                                )
                            }) else null,
                        )
                    }
                }
            }
            if (editing) {
                item(key = "list-actions") {
                    Grouped(Modifier.padding(top = 16.dp)) {
                        ActionRow(stringResource(R.string.rename_list), onClick = { renaming = true })
                        Hairline()
                        ActionRow(stringResource(R.string.move_left), onClick = { scope.launch { repo.moveList(list.id, -1) } }, enabled = selectedIndex > 0)
                        Hairline()
                        ActionRow(stringResource(R.string.move_right), onClick = { scope.launch { repo.moveList(list.id, 1) } }, enabled = selectedIndex < s.lists.lastIndex)
                        Hairline()
                        ActionRow(stringResource(R.string.finance_delete_list), onClick = { deleting = true }, destructive = true)
                    }
                }
            }
            item(key = "add") {
                AddSymbolCard(repo = repo, initialType = list.mainType, targetList = { list.id })
            }
        }

        item(key = "status") {
            Footnote(
                when {
                    s.lastRefresh == 0L -> stringResource(R.string.not_updated_yet)
                    s.lastRefreshFailed -> stringResource(R.string.offline_since, formatTime(context, s.lastRefresh))
                    else -> stringResource(R.string.updated_at, formatTime(context, s.lastRefresh))
                },
                error = s.lastRefreshFailed,
            )
        }

        item { SectionHeader(stringResource(R.string.widgets_title)) }
        item { ModuleWidgetsCard(FinanceModule) }
        item { Footnote(stringResource(R.string.finance_widgets_help)) }
        item { Footnote(stringResource(R.string.sources_note)) }
    }

    if (creating) {
        NameDialog(stringResource(R.string.new_list), "", stringResource(R.string.create), onDismiss = { creating = false }) { name ->
            creating = false
            scope.launch { selectedId = repo.addList(name) }
        }
    }
    if (renaming && list != null) {
        NameDialog(stringResource(R.string.rename_list), list.name, stringResource(R.string.save), onDismiss = { renaming = false }) { name ->
            renaming = false
            scope.launch { repo.renameList(list.id, name) }
        }
    }
    if (deleting && list != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_list_title, list.name),
            text = stringResource(R.string.delete_list_text),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { deleting = false },
        ) {
            deleting = false
            editing = false
            scope.launch { repo.deleteList(list.id) }
            selectedId = null
        }
    }
}


