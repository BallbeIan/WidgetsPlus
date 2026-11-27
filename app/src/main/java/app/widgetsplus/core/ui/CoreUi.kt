package app.widgetsplus.core.ui

import android.appwidget.AppWidgetManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.widgetsplus.R
import app.widgetsplus.core.WidgetModule

/*
 * The design kit: the same components as SmartWallet and Orbit. Screens are a column of grouped cards with
 * small upper-case headers, rows with coloured squircle icons and grey chevrons, and a large bold title on top.
 * Build every screen from these, so the whole app (and every future module) looks like one.
 */

// ---------------------------------------------------------------- page structure

/** True for pages shown above the app's tab bar, which already keeps clear of the navigation bar. */
val LocalInTabs = staticCompositionLocalOf { false }

/**
 * A scrolling page: optional "‹ Back" link, the large title with optional buttons, then [content].
 * Content is full width; [Grouped], [SectionHeader] and [Footnote] add their own side margins.
 */
@Composable
fun Page(
    title: String,
    backLabel: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomPadding: Dp = 24.dp,
    content: LazyListScope.() -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Under the tab bar, the bar already sits above the navigation bar.
    val bottom = if (LocalInTabs.current) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding(),
        contentPadding = PaddingValues(top = top + 4.dp, bottom = bottom + bottomPadding),
    ) {
        if (onBack != null) item { BackButton(backLabel ?: "", onBack) }
        item { LargeTitle(title, trailing = actions) }
        content()
    }
}

/** The big bold title at the top of each screen, with room for buttons on the right. */
@Composable
fun LargeTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.displayMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** "‹ Widgets": the way back from a pushed screen. */
@Composable
fun BackButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier.padding(start = 6.dp, top = 4.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(start = 0.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.back), tint = Accent, modifier = Modifier.size(30.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Accent)
    }
}

/** Small grey upper-case heading above a group. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 7.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        trailing?.invoke()
    }
}

/** Grey explanation under a group, or red for an error. */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier, error: Boolean = false) {
    Text(
        text,
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 7.dp),
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** An inset rounded card: one group of a grouped list. Separate rows inside with [Hairline]. */
@Composable
fun Grouped(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Surface(modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(content = content)
    }

/** The hairline between rows, inset from the left (past the icon, when rows have one). */
@Composable
fun Hairline(startIndent: Dp = 16.dp) =
    HorizontalDivider(Modifier.padding(start = startIndent), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

/** Indent for a [Hairline] under rows that start with a 30dp [Squircle]. */
val IconRowIndent = 58.dp

// ---------------------------------------------------------------- rows

/** A coloured rounded square with a white symbol: the icon badge in front of rows. */
@Composable
fun Squircle(@DrawableRes icon: Int, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.26f))
            .background(Brush.verticalGradient(listOf(lighten(color, 0.18f), color))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

@Composable
fun Squircle(icon: ImageVector, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.26f))
            .background(Brush.verticalGradient(listOf(lighten(color, 0.18f), color))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

private fun lighten(c: Color, by: Float) = Color(c.red + (1 - c.red) * by, c.green + (1 - c.green) * by, c.blue + (1 - c.blue) * by, c.alpha)

@Composable
fun Chevron() = Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))

/** Readable text for something drawn on top of [c]. */
fun onColor(c: Color): Color = if (c.luminance() > 0.55f) Color(0xFF1C1C1E) else Color.White

/** One line in a grouped list: optional badge, a title with an optional second line, and something on the right. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}

/** A row that is itself a button, in the accent colour ("Add event", "New list"). Red when [destructive]. */
@Composable
fun ActionRow(text: String, onClick: () -> Unit, destructive: Boolean = false, enabled: Boolean = true) {
    val color = when {
        !enabled -> MaterialTheme.colorScheme.outline
        destructive -> MaterialTheme.colorScheme.error
        else -> Accent
    }
    Text(
        text,
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = color,
    )
}

/** A row with a check mark when selected: one option of a single choice. */
@Composable
fun CheckRow(title: String, selected: Boolean, onClick: () -> Unit, subtitle: String? = null, leading: (@Composable () -> Unit)? = null) {
    ListRow(title, subtitle = subtitle, onClick = onClick, leading = leading, trailing = {
        if (selected) Icon(Icons.Rounded.Check, null, tint = Accent, modifier = Modifier.size(22.dp))
    })
}

/** The green switch, as in the other apps. */
@Composable
fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val off = if (AppTheme.isDark) Color(0xFF39393D) else Color(0xFFE9E9EA)
    val on = Hue.GREEN.color
    Switch(
        checked, onChange, modifier, enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = on, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = off, uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = on.copy(alpha = 0.4f), disabledUncheckedTrackColor = off.copy(alpha = 0.6f),
        ),
    )
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) =
    ListRow(title, subtitle = subtitle, onClick = { onChange(!checked) }, trailing = { AppSwitch(checked, onChange) })

/** "Corner roundness ··· 16dp" with a slider underneath, as in Orbit's Look screen. */
@Composable
fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    steps: Int = 0,
    onChangeFinished: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyLarge.merge(Num), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Accent,
                inactiveTrackColor = if (AppTheme.isDark) Color(0xFF48484A) else Color(0xFFD1D1D6),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

/** One choice in a [SwatchRow]: a colour, or a two-tone swatch for "follow the phone". */
data class Swatch(val key: String, val label: String, val color: Color, val second: Color? = null)

/** A row of colour circles; the selected one has an accent ring. */
@Composable
fun SwatchRow(title: String, swatches: List<Swatch>, selected: String, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)) {
        Text(title, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            itemsIndexed(swatches, key = { _, s -> s.key }) { _, s ->
                val isOn = s.key == selected
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .then(if (isOn) Modifier.border(2.5.dp, Accent, CircleShape) else Modifier)
                        .clickable(remember { MutableInteractionSource() }, null) { onPick(s.key) }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val fill = if (s.second != null) Brush.linearGradient(listOf(s.color, s.color, s.second, s.second)) else SolidColor(s.color)
                    Box(
                        Modifier.fillMaxSize().clip(CircleShape).background(fill)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                }
            }
        }
    }
}

/** "Text colour ··· Automatic ⇅": opens a bottom sheet of options with a check mark on the current one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PickerRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onPick: (T) -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    ListRow(title, onClick = { open = true }, leading = leading, trailing = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label(selected), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Rounded.UnfoldMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    })
    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                title,
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            options.forEachIndexed { i, o ->
                if (i > 0) Hairline(20.dp)
                CheckRow(label(o), o == selected, onClick = { onPick(o); open = false })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A text field that sits inside a [Grouped] card as a row, without the Material outline. */
@Composable
fun FieldRow(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    onDone: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke()
    }
}

/** A small filled capsule button ("Add"), used at the end of rows. */
@Composable
fun CapsuleButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Text(
        text,
        Modifier.clip(CircleShape)
            .background(if (enabled) Accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) Accent else MaterialTheme.colorScheme.outline,
    )
}

// ---------------------------------------------------------------- dialogs

/** The app's alert: rounded card, accent buttons. Red confirm when [destructive]. */
@Composable
fun AppAlert(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    text: String? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    body: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(26.dp),
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = if (text == null && body == null) null else ({
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                body?.invoke()
            }
        }),
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel, color = if (!confirmEnabled) MaterialTheme.colorScheme.outline else if (destructive) MaterialTheme.colorScheme.error else Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Accent) } },
    )
}

/** A filled grey text box for dialogs. */
@Composable
fun DialogField(value: String, onChange: (String) -> Unit, placeholder: String, capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        FieldRow(value, onChange, placeholder, capitalization = capitalization)
    }
}

/** Asks for a name, for example when creating or renaming a list. */
@Composable
fun NameDialog(title: String, initial: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AppAlert(
        title = title,
        confirmLabel = confirmLabel,
        onConfirm = { onConfirm(name.trim()) },
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(),
        body = { DialogField(name, { name = it }, stringResource(R.string.name), KeyboardCapitalization.Words) },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) =
    AppAlert(title, confirmLabel, onConfirm, onDismiss, text = text, destructive = true)

// ---------------------------------------------------------------- module pieces

/**
 * A module's widget types, each with its squircle, description and an "Add" button that asks the launcher to place
 * it (on launchers that support that). Every module screen shows this, so adding widgets works the same everywhere.
 */
@Composable
fun ModuleWidgetsCard(module: WidgetModule, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    val canPin = manager.isRequestPinAppWidgetSupported
    Grouped(modifier) {
        module.widgets.forEachIndexed { i, kind ->
            if (i > 0) Hairline(IconRowIndent)
            ListRow(
                title = stringResource(kind.name),
                subtitle = stringResource(kind.description),
                leading = { Squircle(kind.icon, module.hue.color) },
                trailing = if (canPin) ({ CapsuleButton(stringResource(R.string.add), onClick = { manager.requestPinAppWidget(kind.component(context), null, null) }) }) else null,
            )
        }
    }
    if (!canPin) Footnote(stringResource(R.string.how_to_add_widget))
}

// ---------------------------------------------------------------- chips and edit mode

/** A capsule choice, as in SmartWallet's account row: filled with the accent when selected. */
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick, modifier, shape = CircleShape, color = if (selected) Accent else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            label,
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** "Edit" / "Done" at the right of a [SectionHeader], switching a list into edit mode. */
@Composable
fun EditToggle(editing: Boolean, onToggle: () -> Unit) {
    Text(
        stringResource(if (editing) R.string.done else R.string.edit),
        style = MaterialTheme.typography.bodyMedium,
        color = Accent,
        modifier = Modifier.padding(start = 8.dp).clickableNoRipple(onToggle),
    )
}

/** The red minus in front of a row in edit mode. */
@Composable
fun RemoveButton(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Rounded.RemoveCircle, contentDescription = description, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
    }
}

/** Up and down arrows at the end of a row in edit mode. */
@Composable
fun MoveButtons(canUp: Boolean, canDown: Boolean, onUp: () -> Unit, onDown: () -> Unit) {
    Row {
        IconButton(onClick = onUp, enabled = canUp, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up), tint = if (canUp) Accent else MaterialTheme.colorScheme.outlineVariant)
        }
        IconButton(onClick = onDown, enabled = canDown, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down), tint = if (canDown) Accent else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Tappable without the grey ripple, for text links like "Set up". */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
