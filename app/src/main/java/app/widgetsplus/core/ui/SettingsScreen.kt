package app.widgetsplus.core.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.widgetsplus.R

private const val REPO = "https://github.com/BallbeIan/WidgetsPlus"

/** App settings and information. */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    Page(stringResource(R.string.tab_settings)) {
        item { SectionHeader(stringResource(R.string.settings_appearance)) }
        item {
            Grouped {
                PickerRow(
                    title = stringResource(R.string.settings_theme),
                    options = AppTheme.Mode.entries,
                    selected = AppTheme.mode,
                    label = { context.getString(themeLabel(it)) },
                    onPick = { AppTheme.set(context, it) },
                    leading = { Squircle(Icons.Rounded.DarkMode, Hue.INDIGO.color) },
                )
            }
        }
        item { Footnote(stringResource(R.string.settings_theme_help)) }

        item { SectionHeader(stringResource(R.string.settings_widgets)) }
        item {
            Grouped {
                ListRow(
                    title = stringResource(R.string.settings_updates),
                    subtitle = stringResource(R.string.settings_updates_help),
                    leading = { Squircle(Icons.Rounded.Sync, Hue.TEAL.color) },
                )
            }
        }

        item { SectionHeader(stringResource(R.string.settings_about)) }
        item {
            Grouped {
                ListRow(
                    stringResource(R.string.settings_privacy),
                    onClick = { open("$REPO/blob/main/docs/PRIVACY.md") },
                    leading = { Squircle(Icons.Rounded.Lock, Hue.BLUE.color) },
                    trailing = { Chevron() },
                )
                Hairline(IconRowIndent)
                ListRow(
                    stringResource(R.string.settings_report),
                    onClick = { open("$REPO/issues") },
                    leading = { Squircle(Icons.Rounded.BugReport, Hue.RED.color) },
                    trailing = { Chevron() },
                )
                Hairline(IconRowIndent)
                ListRow(
                    stringResource(R.string.settings_source),
                    onClick = { open(REPO) },
                    leading = { Squircle(Icons.Rounded.Code, Hue.GRAY.color) },
                    trailing = { Chevron() },
                )
                Hairline(IconRowIndent)
                ListRow(
                    stringResource(R.string.settings_licences),
                    subtitle = stringResource(R.string.settings_licences_help),
                    onClick = { open("$REPO/tree/main/docs/licenses") },
                    leading = { Squircle(Icons.Rounded.Description, Hue.ORANGE.color) },
                    trailing = { Chevron() },
                )
            }
        }
        item { Footnote(stringResource(R.string.settings_version, version)) }
    }
}

private fun themeLabel(m: AppTheme.Mode) = when (m) {
    AppTheme.Mode.SYSTEM -> R.string.settings_theme_system
    AppTheme.Mode.LIGHT -> R.string.settings_theme_light
    AppTheme.Mode.DARK -> R.string.settings_theme_dark
}
