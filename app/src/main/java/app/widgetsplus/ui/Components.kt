package app.widgetsplus.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.widgetsplus.R
import app.widgetsplus.data.AssetType
import app.widgetsplus.data.Format
import app.widgetsplus.data.PriceSources
import app.widgetsplus.data.Quote
import app.widgetsplus.data.Repository
import app.widgetsplus.data.UnknownSymbolException
import app.widgetsplus.data.WatchItem
import kotlinx.coroutines.launch

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun changeColor(change: Double): Color {
    val dark = isSystemInDarkTheme()
    return if (change >= 0) if (dark) Color(0xFF4CD07D) else Color(0xFF1B873F)
    else if (dark) Color(0xFFFF6B6B) else Color(0xFFC62828)
}

/** Symbol and name on the left, price and change on the right, then any [trailing] buttons. */
@Composable
fun ItemSummary(item: WatchItem, quote: Quote?, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                item.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(quote?.let { Format.price(it.price, it.currency) } ?: "—", style = MaterialTheme.typography.bodyLarge)
            if (quote != null) {
                Text(Format.percent(quote.changePercent), style = MaterialTheme.typography.bodySmall, color = changeColor(quote.change))
            }
        }
        trailing()
    }
}

/** Type picker, symbol field and Add button. The symbol is checked online before it is added. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSymbolCard(repo: Repository, modifier: Modifier = Modifier, onAdded: (WatchItem) -> Unit = {}) {
    var type by remember { mutableStateOf(AssetType.STOCK) }
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
                if (repo.state.value.items.any { it.key == item.key }) {
                    error = context.getString(R.string.already_in_list, item.label)
                } else {
                    repo.add(item, runCatching { PriceSources.quote(item) }.getOrNull())
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

    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.add_symbol), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AssetType.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t; error = null },
                        shape = SegmentedButtonDefaults.itemShape(i, AssetType.entries.size),
                    ) { Text(stringResource(if (t == AssetType.STOCK) R.string.type_stock else R.string.type_crypto)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.symbol)) },
                    placeholder = { Text(stringResource(if (type == AssetType.STOCK) R.string.hint_stock else R.string.hint_crypto)) },
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                )
                Spacer(Modifier.width(12.dp))
                Button(onClick = { add() }, enabled = text.isNotBlank() && !busy) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.add))
                }
            }
            Text(
                error ?: stringResource(if (type == AssetType.STOCK) R.string.help_stock else R.string.help_crypto),
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
