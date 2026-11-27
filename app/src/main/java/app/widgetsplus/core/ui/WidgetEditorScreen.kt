package app.widgetsplus.core.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.widgetsplus.R
import app.widgetsplus.core.Design
import app.widgetsplus.core.Modules
import app.widgetsplus.core.PlacedWidget
import app.widgetsplus.core.TextTone
import app.widgetsplus.core.WidgetBackgrounds
import app.widgetsplus.core.WidgetStyle
import app.widgetsplus.core.WidgetStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.widgetsplus.core.widget.PreviewStyle
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One widget's editor: a live preview (the real widget, drawn by the app), what it shows, and how it looks.
 * Every change is saved and applied to the widget on the home screen straight away.
 */
@Composable
fun WidgetEditorScreen(appWidgetId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Re-read when coming back from the widget's own configure screen.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val placed = remember(resumes) { Modules.placed(context, appWidgetId) }
    if (placed == null) {
        // The widget was removed from the home screen meanwhile.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var style by remember { mutableStateOf(WidgetStyles.of(context, appWidgetId)) }
    DisposableEffect(Unit) { onDispose { PreviewStyle.override = null } }
    val description = remember(resumes) { placed.module.describeWidget(context, appWidgetId, placed.kind) }

    /** Saves the style and redraws the widget on the home screen. */
    fun apply(new: WidgetStyle) {
        style = new
        if (WidgetStyles.own(context, appWidgetId) == new) return
        WidgetStyles.set(context, appWidgetId, new)
        scope.launch { redraw(context, placed) }
    }

    Page(stringResource(placed.kind.name), backLabel = stringResource(R.string.tab_widgets), onBack = onBack) {
        item { LivePreview(placed, style, resumes) }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            item { Footnote(stringResource(R.string.editor_preview_old_android)) }
        }

        val configure = placed.kind.configure
        item { SectionHeader(stringResource(R.string.editor_content)) }
        item {
            Grouped {
                ListRow(
                    title = description,
                    subtitle = stringResource(placed.module.title),
                    onClick = configure?.let { cls ->
                        { context.startActivity(Intent(context, cls).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)) }
                    },
                    leading = { Squircle(placed.kind.icon, placed.module.hue.color) },
                    trailing = if (configure != null) ({ Chevron() }) else null,
                )
            }
        }

        item { SectionHeader(stringResource(R.string.editor_design)) }
        item {
            Grouped {
                Design.entries.forEachIndexed { i, d ->
                    if (i > 0) Hairline()
                    CheckRow(
                        title = stringResource(d.label),
                        subtitle = stringResource(d.description),
                        selected = style.design == d,
                        onClick = { apply(style.copy(design = d)) },
                    )
                }
            }
        }

        item { SectionHeader(stringResource(R.string.editor_colour)) }
        item {
            Grouped {
                SwatchRow(
                    title = stringResource(R.string.editor_background),
                    swatches = WidgetBackgrounds.all.map { bg ->
                        val label = context.getString(bg.label)
                        if (bg.argb == null) Swatch(bg.key, label, Color(0xFFF2F2F7), Color(0xFF1C1C1E))
                        else Swatch(bg.key, label, Color(bg.argb))
                    },
                    selected = style.background,
                    onPick = { apply(style.copy(background = it)) },
                )
                Hairline()
                SliderRow(
                    title = stringResource(R.string.editor_opacity),
                    valueText = "${style.opacity}%",
                    value = style.opacity.toFloat(),
                    range = 0f..100f,
                    // While dragging only the preview changes; the widget is saved and redrawn on release.
                    onChange = { style = style.copy(opacity = (it / 5).roundToInt() * 5) },
                    onChangeFinished = { apply(style) },
                )
                Hairline()
                PickerRow(
                    title = stringResource(R.string.editor_text),
                    options = TextTone.entries,
                    selected = style.text,
                    label = { context.getString(it.label) },
                    onPick = { apply(style.copy(text = it)) },
                )
            }
        }
        item { Footnote(stringResource(R.string.editor_colour_help)) }

        item { SectionHeader(stringResource(R.string.editor_shape)) }
        item {
            Grouped {
                SliderRow(
                    title = stringResource(R.string.editor_corners),
                    valueText = "${style.corners}dp",
                    value = style.corners.toFloat(),
                    range = 0f..WidgetStyle.MAX_CORNERS.toFloat(),
                    onChange = { style = style.copy(corners = it.roundToInt()) },
                    onChangeFinished = { apply(style) },
                )
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            item { Footnote(stringResource(R.string.editor_corners_old_android)) }
        }

        item { SectionHeader("") }
        item {
            Grouped {
                ActionRow(stringResource(R.string.editor_apply_all), onClick = {
                    scope.launch {
                        Modules.placedWidgets(context).forEach { other ->
                            WidgetStyles.set(context, other.appWidgetId, style)
                            redraw(context, other)
                        }
                    }
                })
                Hairline()
                ActionRow(stringResource(R.string.editor_reset), onClick = {
                    WidgetStyles.reset(context, appWidgetId)
                    style = WidgetStyles.of(context, appWidgetId)
                    scope.launch { redraw(context, placed) }
                }, destructive = true)
            }
        }
        item { Footnote(stringResource(R.string.editor_apply_all_help)) }
    }
}

/** Redraws one placed widget on the home screen. */
private suspend fun redraw(context: Context, w: PlacedWidget) {
    runCatching {
        val id = GlanceAppWidgetManager(context).getGlanceIdBy(w.appWidgetId)
        w.kind.glanceWidget().update(context, id)
    }
}

/** The widget's size on the home screen, in portrait, from the launcher's options. */
private fun placedSize(context: Context, appWidgetId: Int): DpSize {
    val o = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (w > 0 && h > 0) DpSize(w.dp, h.dp) else DpSize(250.dp, 130.dp)
}

/**
 * The widget as it looks right now, drawn by Glance into the app on a wallpaper-like background. Taps are blocked,
 * so the preview can't open other apps.
 */
@OptIn(ExperimentalGlanceApi::class)
@Composable
private fun LivePreview(placed: PlacedWidget, style: WidgetStyle, refreshKey: Int) {
    val context = LocalContext.current
    val size = remember(refreshKey) { placedSize(context, placed.appWidgetId) }
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(style, refreshKey) {
        delay(60) // While a slider is dragged, only draw once it pauses.
        views = withContext(Dispatchers.Default) {
            runCatching {
                PreviewStyle.override = placed.appWidgetId to style
                val id = GlanceAppWidgetManager(context).getGlanceIdBy(placed.appWidgetId)
                placed.kind.glanceWidget().compose(context, id, size = size)
            }.getOrNull()
        }
    }
    val backdrop = if (AppTheme.isDark) listOf(Color(0xFF1E2A5A), Color(0xFF4A2A6A), Color(0xFF7A3550))
    else listOf(Color(0xFF8FB3FF), Color(0xFFB49CFF), Color(0xFFFFB3C7))

    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(backdrop)),
        contentAlignment = Alignment.Center,
    ) {
        // Shrink large widgets to fit the preview area.
        val scale = min(1f, min((maxWidth - 32.dp) / size.width, (maxHeight - 32.dp) / size.height))
        val rv = views
        if (rv == null) {
            Text(stringResource(R.string.editor_preview_loading), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        } else {
            Box(Modifier.requiredSize(size).graphicsLayer(scaleX = scale, scaleY = scale)) {
                AndroidView(
                    factory = { FrameLayout(it) },
                    update = { frame ->
                        frame.removeAllViews()
                        runCatching { frame.addView(rv.apply(frame.context, frame)) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                // Swallow taps so the preview doesn't launch anything.
                Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { })
            }
        }
    }
}
