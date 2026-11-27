package app.widgetsplus.finance.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import app.widgetsplus.R
import app.widgetsplus.core.ui.ConfirmDialog
import app.widgetsplus.core.ui.ModuleWidgetsCard
import app.widgetsplus.core.ui.NameDialog
import app.widgetsplus.core.ui.SectionTitle
import app.widgetsplus.core.widget.formatTime
import app.widgetsplus.finance.FinanceModule
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.PriceList
import kotlinx.coroutines.launch

/** Finance settings: one tab per list, the symbols in the selected list, and the finance widgets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(onBack: () -> Unit) {
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

    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.finance_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
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
                        IconButton(onClick = { creating = true }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_list))
                        }
                    },
                )
                if (s.lists.isNotEmpty()) {
                    PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 16.dp) {
                        s.lists.forEachIndexed { i, l ->
                            Tab(selected = i == selectedIndex, onClick = { selectedId = l.id }, text = { Text(l.name) })
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    when {
                        s.lastRefresh == 0L -> stringResource(R.string.not_updated_yet)
                        s.lastRefreshFailed -> stringResource(R.string.offline_since, formatTime(context, s.lastRefresh))
                        else -> stringResource(R.string.updated_at, formatTime(context, s.lastRefresh))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.lastRefreshFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (list == null) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.no_lists), style = MaterialTheme.typography.bodyMedium)
                            FilledTonalButton(onClick = { creating = true }, modifier = Modifier.padding(top = 12.dp)) {
                                Text(stringResource(R.string.new_list))
                            }
                        }
                    }
                }
            } else {
                item {
                    ListCard(
                        list = list,
                        repo = repo,
                        canMoveLeft = selectedIndex > 0,
                        canMoveRight = selectedIndex < s.lists.lastIndex,
                        onRename = { renaming = true },
                        onDelete = { deleting = true },
                    )
                }
                item {
                    AddSymbolCard(
                        repo = repo,
                        initialType = list.mainType,
                        targetList = { list.id },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.widgets_title)) }
            item { ModuleWidgetsCard(FinanceModule, Modifier.fillMaxWidth()) }
            item {
                Text(
                    stringResource(R.string.finance_widgets_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    stringResource(R.string.sources_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
            scope.launch { repo.deleteList(list.id) }
            selectedId = null
        }
    }
}

/** The symbols in one list, with reorder and remove buttons, and a menu for the list itself. */
@Composable
private fun ListCard(
    list: PriceList,
    repo: FinanceRepository,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val s by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(list.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.list_options))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { menu = false; onRename() })
                    if (canMoveLeft) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.move_left)) }, onClick = {
                            menu = false
                            scope.launch { repo.moveList(list.id, -1) }
                        })
                    }
                    if (canMoveRight) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.move_right)) }, onClick = {
                            menu = false
                            scope.launch { repo.moveList(list.id, 1) }
                        })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        if (list.items.isEmpty()) {
            Text(
                stringResource(R.string.finance_list_empty_app),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
        list.items.forEachIndexed { i, item ->
            HorizontalDivider()
            ItemSummary(
                item = item,
                quote = s.quotes[item.key],
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Column {
                    IconButton(onClick = { scope.launch { repo.moveItem(list.id, item.key, -1) } }, enabled = i > 0, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                    }
                    IconButton(onClick = { scope.launch { repo.moveItem(list.id, item.key, 1) } }, enabled = i < list.items.lastIndex, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                    }
                }
                IconButton(onClick = { scope.launch { repo.removeItem(list.id, item.key) } }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove, item.label))
                }
            }
        }
    }
}
