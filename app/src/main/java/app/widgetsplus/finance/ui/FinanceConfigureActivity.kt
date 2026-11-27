package app.widgetsplus.finance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.Accent
import app.widgetsplus.core.ui.ActionRow
import app.widgetsplus.core.ui.CheckRow
import app.widgetsplus.core.ui.ConfigureScaffold
import app.widgetsplus.core.ui.Footnote
import app.widgetsplus.core.ui.Grouped
import app.widgetsplus.core.ui.Hairline
import app.widgetsplus.core.ui.NameDialog
import app.widgetsplus.core.ui.SectionHeader
import app.widgetsplus.core.ui.WidgetConfigureActivity
import app.widgetsplus.finance.AssetType
import app.widgetsplus.finance.FinanceRepository
import androidx.glance.appwidget.GlanceAppWidget
import app.widgetsplus.finance.widget.ChartWidget
import app.widgetsplus.finance.widget.ChartWidgetReceiver
import app.widgetsplus.finance.widget.MiniListWidget
import app.widgetsplus.finance.widget.MiniListWidgetReceiver
import app.widgetsplus.finance.widget.PriceListWidget
import app.widgetsplus.finance.widget.PriceListWidgetReceiver
import app.widgetsplus.finance.widget.SymbolWidget
import kotlinx.coroutines.launch

/** Chooses what a finance widget shows: a list for the list widgets, a symbol for the price and chart widgets. */
class FinanceConfigureActivity : WidgetConfigureActivity() {

    private val isList get() = receiverClassName == PriceListWidgetReceiver::class.java.name || receiverClassName == MiniListWidgetReceiver::class.java.name
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
            item { SectionHeader(stringResource(R.string.finance_lists)) }
            item {
                Grouped {
                    s.lists.forEach { list ->
                        val count = pluralStringResource(R.plurals.symbols, list.items.size, list.items.size)
                        CheckRow(
                            title = list.name,
                            selected = list.id == current,
                            subtitle = if (list.items.isEmpty()) getString(R.string.empty_list) else "$count · " + list.items.joinToString(", ") { it.label },
                            onClick = { choose { repo.setListForWidget(appWidgetId, list.id) } },
                        )
                        Hairline()
                    }
                    ActionRow(stringResource(R.string.new_list), onClick = { creating = true })
                }
            }
            item { Footnote(stringResource(R.string.choose_list_help)) }
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
                item(key = "header-${list.id}") { SectionHeader(list.name) }
                item(key = "items-${list.id}") {
                    Grouped {
                        list.items.forEachIndexed { i, item ->
                            if (i > 0) Hairline()
                            ItemSummary(
                                item = item,
                                quote = s.quotes[item.key],
                                modifier = Modifier.clickable { choose { repo.setItemForWidget(appWidgetId, item.key) } },
                                trailing = {
                                    // Always takes the space, so prices line up whether or not the row is ticked.
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (item.key == current) Accent else Color.Transparent,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                            )
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
                    onAdded = { item -> choose { repo.setItemForWidget(appWidgetId, item.key) } },
                )
            }
        }
    }

    private fun choose(save: () -> Unit) = finishWith(widget(), save)

    /** The widget to redraw. List widgets share the list setting, symbol widgets the symbol setting. */
    private fun widget(): GlanceAppWidget = when (receiverClassName) {
        PriceListWidgetReceiver::class.java.name -> PriceListWidget()
        MiniListWidgetReceiver::class.java.name -> MiniListWidget()
        ChartWidgetReceiver::class.java.name -> ChartWidget()
        else -> SymbolWidget()
    }
}
