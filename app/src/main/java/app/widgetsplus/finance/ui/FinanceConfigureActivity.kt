package app.widgetsplus.finance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.ChoiceCard
import app.widgetsplus.core.ui.ConfigureScaffold
import app.widgetsplus.core.ui.NameDialog
import app.widgetsplus.core.ui.SectionTitle
import app.widgetsplus.core.ui.WidgetConfigureActivity
import app.widgetsplus.finance.AssetType
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.widget.PriceListWidget
import app.widgetsplus.finance.widget.PriceListWidgetReceiver
import app.widgetsplus.finance.widget.SymbolWidget
import kotlinx.coroutines.launch

/** Chooses what a finance widget shows: a list for the list widget, a symbol for the single price widget. */
class FinanceConfigureActivity : WidgetConfigureActivity() {

    private val isList get() = receiverClassName == PriceListWidgetReceiver::class.java.name
    private val repo by lazy { FinanceRepository.get(this) }

    @Composable
    override fun Content() = if (isList) ChooseList() else ChooseSymbol()

    @Composable
    private fun ChooseList() {
        val s by repo.state.collectAsStateWithLifecycle()
        val current = remember { repo.listForWidget(appWidgetId)?.id }
        var creating by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        ConfigureScaffold(stringResource(R.string.choose_list)) {
            items(s.lists, key = { it.id }) { list ->
                ChoiceCard(
                    selected = list.id == current,
                    title = list.name,
                    subtitle = if (list.items.isEmpty()) getString(R.string.empty_list) else list.items.joinToString(", ") { it.label },
                    onClick = { choose { repo.setListForWidget(appWidgetId, list.id) } },
                ) {
                    Text(
                        pluralStringResource(R.plurals.symbols, list.items.size, list.items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
        if (creating) {
            NameDialog(stringResource(R.string.new_list), "", stringResource(R.string.create), onDismiss = { creating = false }) { name ->
                creating = false
                scope.launch {
                    val id = repo.addList(name)
                    choose { repo.setListForWidget(appWidgetId, id) }
                }
            }
        }
    }

    @Composable
    private fun ChooseSymbol() {
        val s by repo.state.collectAsStateWithLifecycle()
        val current = remember { repo.itemForWidget(appWidgetId)?.key }

        ConfigureScaffold(stringResource(R.string.choose_symbol)) {
            s.lists.filter { it.items.isNotEmpty() }.forEach { list ->
                item(key = "header-${list.id}") { SectionTitle(list.name) }
                items(list.items, key = { "${list.id}-${it.key}" }) { item ->
                    Card(Modifier.fillMaxWidth().clickable { choose { repo.setItemForWidget(appWidgetId, item.key) } }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = item.key == current, onClick = { choose { repo.setItemForWidget(appWidgetId, item.key) } })
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
                    onAdded = { item -> choose { repo.setItemForWidget(appWidgetId, item.key) } },
                )
            }
        }
    }

    private fun choose(save: () -> Unit) = finishWith(if (isList) PriceListWidget() else SymbolWidget(), save)
}
