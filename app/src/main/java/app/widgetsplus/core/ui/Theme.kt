package app.widgetsplus.core.ui

import android.content.Context
import androidx.activity.SystemBarStyle
import android.content.res.Configuration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.widgetsplus.R

/*
 * The look shared with the developer's other apps (SmartWallet, Orbit): iOS-style grouped screens, Inter, pure black
 * in dark mode and #F2F2F7 in light mode, bright system-style hues. WidgetsPlus's own accent is indigo.
 */

/** Dark, light, or follow the phone (the default). Compose state, so changing it redraws the whole app. */
object AppTheme {
    enum class Mode { SYSTEM, LIGHT, DARK }

    var mode by mutableStateOf(Mode.SYSTEM)
        private set
    private var systemDark by mutableStateOf(true)

    val isDark: Boolean get() = when (mode) { Mode.SYSTEM -> systemDark; Mode.LIGHT -> false; Mode.DARK -> true }

    /** Call from every activity's onCreate (and on configuration changes) before drawing. */
    fun load(c: Context) {
        mode = c.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("theme", null)
            ?.let { name -> Mode.entries.firstOrNull { it.name == name } } ?: Mode.SYSTEM
        systemDark = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /** Status and navigation bar icons that suit the current theme, for enableEdgeToEdge. */
    fun barStyle(): SystemBarStyle =
        if (isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)

    fun set(c: Context, m: Mode) {
        c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("theme", m.name).apply()
        mode = m
    }
}

/** The bright system hues, as day/night pairs. Categories and icons are coloured with these. */
enum class Hue(private val day: Long, private val night: Long) {
    INDIGO(0xFF5856D6, 0xFF5E5CE6),
    BLUE(0xFF007AFF, 0xFF0A84FF),
    GREEN(0xFF34C759, 0xFF30D158),
    ORANGE(0xFFFF9500, 0xFFFF9F0A),
    RED(0xFFFF3B30, 0xFFFF453A),
    PINK(0xFFFF2D55, 0xFFFF375F),
    PURPLE(0xFFAF52DE, 0xFFBF5AF2),
    TEAL(0xFF30B0C7, 0xFF40C8E0),
    GRAY(0xFF8E8E93, 0xFF8E8E93);

    val color: Color get() = Color(if (AppTheme.isDark) night else day)
}

/** WidgetsPlus's accent. */
val Accent: Color get() = Hue.INDIGO.color
/** Prices going up and down, readable as text on both backgrounds. */
val UpText: Color get() = if (AppTheme.isDark) Color(0xFF30D158) else Color(0xFF248A3D)
val DownText: Color get() = if (AppTheme.isDark) Color(0xFFFF453A) else Color(0xFFD70015)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5E5CE6), onPrimary = Color.White,
    primaryContainer = Color(0xFF25245C), onPrimaryContainer = Color(0xFFB9B8FF),
    secondaryContainer = Color(0xFF2C2C2E), onSecondaryContainer = Color.White,
    background = Color(0xFF000000), onBackground = Color.White,
    surface = Color(0xFF000000), onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFF98989F),
    surfaceContainerLowest = Color(0xFF1C1C1E), surfaceContainerLow = Color(0xFF1C1C1E), surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E), surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF636366), outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF453A), onError = Color.White,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5856D6), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E6FA), onPrimaryContainer = Color(0xFF2B2A8C),
    secondaryContainer = Color(0xFFE5E5EA), onSecondaryContainer = Color.Black,
    background = Color(0xFFF2F2F7), onBackground = Color.Black,
    surface = Color(0xFFF2F2F7), onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA), onSurfaceVariant = Color(0xFF6C6C70),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White, surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFE5E5EA), surfaceContainerHighest = Color(0xFFD1D1D6),
    outline = Color(0xFFAEAEB2), outlineVariant = Color(0xFFD1D1D6),
    error = Color(0xFFFF3B30), onError = Color.White,
)

// Inter is an open font made for screens. It is bundled, so the app needs no internet to show it.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, spacing: Double = 0.0, lineHeight: Int = (size * 1.25).toInt()) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.sp, letterSpacing = spacing.sp, lineHeight = lineHeight.sp)

// The type scale: Large Title 34, Title 2 22, Headline 17, Body 17, Subheadline 15, Footnote 13, Caption 12.
private val Type = Typography(
    displayLarge = style(57, FontWeight.Bold, -0.5), displayMedium = style(34, FontWeight.Bold, -0.4), displaySmall = style(44, FontWeight.Bold, -0.6),
    headlineLarge = style(32, FontWeight.Bold), headlineMedium = style(28, FontWeight.Bold, -0.3), headlineSmall = style(24, FontWeight.SemiBold),
    titleLarge = style(22, FontWeight.Bold, -0.2), titleMedium = style(17, FontWeight.SemiBold, -0.2), titleSmall = style(15, FontWeight.SemiBold),
    bodyLarge = style(17, FontWeight.Normal, -0.2), bodyMedium = style(15, FontWeight.Normal, -0.1), bodySmall = style(13, FontWeight.Normal),
    labelLarge = style(15, FontWeight.Medium), labelMedium = style(12, FontWeight.Medium), labelSmall = style(12, FontWeight.Normal, 0.4),
)

/** Tabular figures keep prices and temperatures in a column aligned. */
val Num = TextStyle(fontFeatureSettings = "tnum")

/** The app's theme. Every screen and configure activity is drawn inside this. */
@Composable
fun WidgetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (AppTheme.isDark) DarkScheme else LightScheme,
        typography = Type,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
