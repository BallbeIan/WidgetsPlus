package app.widgetsplus.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.widgetsplus.core.ui.Footnote
import app.widgetsplus.core.ui.Chip
import app.widgetsplus.R
import app.widgetsplus.core.ui.Accent
import app.widgetsplus.core.ui.CapsuleButton
import app.widgetsplus.core.ui.DownText
import app.widgetsplus.core.ui.FieldRow
import app.widgetsplus.core.ui.Grouped
import app.widgetsplus.core.ui.Hairline
import app.widgetsplus.core.ui.ListRow
import app.widgetsplus.core.ui.Num
import app.widgetsplus.core.ui.SectionHeader
import app.widgetsplus.core.ui.UpText
import app.widgetsplus.finance.AssetType
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.PriceSources
import app.widgetsplus.finance.Quote
import app.widgetsplus.finance.UnknownSymbolException
import app.widgetsplus.finance.WatchItem
import kotlinx.coroutines.launch

/** Price up or down, as text that reads well on the app's background. */
@Composable
fun changeColor(change: Double): Color = if (change >= 0) UpText else DownText

/**
 * One symbol as a row of a grouped list: [leading], symbol and name, then price and change (when [showQuote]),
 * then [trailing].
 */
@Composable
fun ItemSummary(
    item: WatchItem,
    quote: Quote?,
    modifier: Modifier = Modifier,
    showQuote: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showQuote) {
            Column(Modifier.padding(start = 8.dp), horizontalAlignment = Alignment.End) {
                Text(
                    quote?.let { FinanceFormat.price(it.price, it.currency) } ?: "–",
                    style = MaterialTheme.typography.bodyLarge.merge(Num),
                    maxLines = 1,
                )
                if (quote != null) {
                    Text(
                        FinanceFormat.percent(quote.changePercent),
                        style = MaterialTheme.typography.bodySmall.merge(Num),
                        fontWeight = FontWeight.Medium,
                        color = changeColor(quote.change),
                    )
                }
            }
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}



/**
 * "Add a stock or coin": a header, a grouped card with the type and the symbol field with an Add button, and the
 * help or error underneath. The symbol is checked online before it is added.
 * [targetList] picks the list to add to, given the chosen type.
 */
@Composable
fun AddSymbolCard(
    repo: FinanceRepository,
    initialType: AssetType,
    targetList: suspend (AssetType) -> String,
    modifier: Modifier = Modifier,
    onAdded: (WatchItem) -> Unit = {},
) {
    var type by remember(initialType) { mutableStateOf(initialType) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun add() {
        if (text.isBlank() || busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val item = PriceSources.lookUp(type, text)
                val listId = targetList(type)
                val list = repo.state.value.lists.firstOrNull { it.id == listId }
                if (list != null && list.items.any { it.key == item.key }) {
                    error = context.getString(R.string.already_in_list, item.label, list.name)
                } else {
                    repo.addItem(listId, item, runCatching { PriceSources.quote(item) }.getOrNull())
                    text = ""
                    onAdded(item)
                }
            } catch (e: UnknownSymbolException) {
                error = context.getString(if (type == AssetType.STOCK) R.string.not_found_stock else R.string.not_found_crypto)
            } catch (e: Exception) {
                error = context.getString(R.string.no_connection)
            } finally {
                busy = false
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.add_symbol))
        Grouped {
            ListRow(stringResource(R.string.finance_type), trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssetType.entries.forEach { t ->
                        Chip(
                            stringResource(if (t == AssetType.STOCK) R.string.type_stock else R.string.type_crypto),
                            selected = type == t,
                            onClick = { type = t; error = null },
                        )
                    }
                }
            })
            Hairline()
            FieldRow(
                value = text,
                onChange = { text = it; error = null },
                placeholder = stringResource(R.string.finance_symbol_placeholder, stringResource(if (type == AssetType.STOCK) R.string.hint_stock else R.string.hint_crypto)),
                capitalization = KeyboardCapitalization.Characters,
                onDone = { add() },
                trailing = {
                    Spacer(Modifier.width(8.dp))
                    if (busy) CircularProgressIndicator(Modifier.padding(horizontal = 14.dp).size(18.dp), color = Accent, strokeWidth = 2.dp)
                    else CapsuleButton(stringResource(R.string.add), onClick = { add() }, enabled = text.isNotBlank())
                },
            )
        }
        Footnote(
            error ?: stringResource(if (type == AssetType.STOCK) R.string.help_stock else R.string.help_crypto),
            error = error != null,
        )
    }
}
