package app.widgetsplus.core.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import androidx.glance.unit.ColorProvider
import app.widgetsplus.core.TextTone
import app.widgetsplus.core.WidgetBackgrounds
import app.widgetsplus.core.WidgetStyle
import app.widgetsplus.core.WidgetStyles

/**
 * The style being edited, while a slider in the editor is dragged and not yet saved, so the live preview can show it.
 * Cleared when the editor closes.
 */
object PreviewStyle {
    @Volatile var override: Pair<Int, WidgetStyle>? = null
}

/** The style of the widget being drawn. [WidgetSurface] reads it for the corners. */
val LocalWidgetStyle = staticCompositionLocalOf { WidgetStyle() }

/**
 * Wraps a widget's content in its style. Every widget's provideContent uses this instead of GlanceTheme, so all
 * widgets get the same customisation: the GlanceTheme colours are replaced (background with opacity, text colours),
 * and running widgets redraw as soon as their style changes.
 */
@Composable
fun StyledWidget(appWidgetId: Int, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val version by WidgetStyles.version(context).collectAsState()
    val saved = remember(version) { WidgetStyles.of(context, appWidgetId) }
    val style = PreviewStyle.override?.takeIf { it.first == appWidgetId }?.second ?: saved
    GlanceTheme {
        val colors = styledColors(context, GlanceTheme.colors, style)
        CompositionLocalProvider(LocalWidgetStyle provides style) {
            GlanceTheme(colors = colors, content = content)
        }
    }
}

/**
 * Whether the widget's background is dark, light, or null when it follows the phone's dark mode. Colours that
 * have day and night versions (prices up and down, charts) use this to stay readable on fixed backgrounds.
 */
fun backgroundIsDark(style: WidgetStyle): Boolean? = fixedTextColors(style)?.let { it.first == Color.White.toArgb() }

/**
 * Concrete text colours for widget parts drawn with plain views (the clocks), or null when the widget follows the
 * phone's colours, in which case those views keep their day/night colour resources.
 */
fun fixedTextColors(style: WidgetStyle): Pair<Int, Int>? {
    val light = when (style.text) {
        TextTone.LIGHT -> true
        TextTone.DARK -> false
        TextTone.AUTO -> {
            val bg = WidgetBackgrounds.byKey(style.background)?.argb ?: return null
            if (style.opacity < 40) return null
            Color(bg).luminance() < 0.5f
        }
    }
    return if (light) Color.White.toArgb() to Color(0xB3FFFFFF).toArgb() else Color(0xFF111111).toArgb() to Color(0x99000000).toArgb()
}

private fun styledColors(context: Context, base: ColorProviders, style: WidgetStyle): ColorProviders {
    val alpha = style.opacity / 100f
    val fixedBg = WidgetBackgrounds.byKey(style.background)?.argb?.let { Color(it) }
    val background: ColorProvider = if (fixedBg != null) {
        androidx.glance.unit.ColorProvider(fixedBg.copy(alpha = alpha))
    } else if (style.opacity == 100) {
        // Unchanged: the launcher keeps looking this colour up, so dark mode and wallpaper changes apply live.
        base.widgetBackground
    } else {
        // The phone's widget colour, resolved for day and night so dark mode still switches it.
        ColorProvider(
            day = base.widgetBackground.getColor(nightContext(context, false)).copy(alpha = alpha),
            night = base.widgetBackground.getColor(nightContext(context, true)).copy(alpha = alpha),
        )
    }
    val text = fixedTextColors(style)
    val onSurface = text?.let { androidx.glance.unit.ColorProvider(Color(it.first)) } ?: base.onSurface
    val onSurfaceVariant = text?.let { androidx.glance.unit.ColorProvider(Color(it.second)) } ?: base.onSurfaceVariant
    return colorProviders(
        primary = base.primary, onPrimary = base.onPrimary,
        primaryContainer = base.primaryContainer, onPrimaryContainer = base.onPrimaryContainer,
        secondary = base.secondary, onSecondary = base.onSecondary,
        secondaryContainer = base.secondaryContainer, onSecondaryContainer = base.onSecondaryContainer,
        tertiary = base.tertiary, onTertiary = base.onTertiary,
        tertiaryContainer = base.tertiaryContainer, onTertiaryContainer = base.onTertiaryContainer,
        error = base.error, errorContainer = base.errorContainer, onError = base.onError, onErrorContainer = base.onErrorContainer,
        background = base.background, onBackground = base.onBackground,
        surface = base.surface, onSurface = onSurface,
        surfaceVariant = base.surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        outline = base.outline, inverseOnSurface = base.inverseOnSurface, inverseSurface = base.inverseSurface,
        inversePrimary = base.inversePrimary, widgetBackground = background,
    )
}

private fun nightContext(context: Context, night: Boolean): Context {
    val config = Configuration(context.resources.configuration)
    config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return context.createConfigurationContext(config)
}
