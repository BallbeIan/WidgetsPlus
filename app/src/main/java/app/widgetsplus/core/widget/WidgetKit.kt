package app.widgetsplus.core.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.widgetsplus.R
import app.widgetsplus.core.ui.MainActivity
import java.util.Date

/*
 * Building blocks shared by every widget, so all modules look like one family:
 * same background, corners, padding, header and refresh button.
 */

/** The rounded, themed background every widget sits on. */
@Composable
fun WidgetSurface(
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = GlanceModifier.fillMaxSize()
        .appWidgetBackground()
        .cornerRadius(20.dp)
        .background(GlanceTheme.colors.widgetBackground)
    if (onClick != null) m = m.clickable(onClick)
    Column(modifier = m.then(modifier), content = content)
}

/** Title on the left; update time and a refresh button on the right. */
@Composable
fun WidgetHeader(title: String, status: String?, onTitleClick: Action?, refresh: Action?, refreshing: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        var titleModifier: GlanceModifier = GlanceModifier
        if (onTitleClick != null) titleModifier = titleModifier.clickable(onTitleClick)
        Text(
            title,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = titleModifier,
        )
        Spacer(GlanceModifier.defaultWeight())
        if (status != null) {
            Text(status, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
        }
        if (refresh != null) RefreshButton(refresh, refreshing)
    }
}

@Composable
fun RefreshButton(action: Action, refreshing: Boolean) {
    Box(
        modifier = GlanceModifier.size(28.dp).cornerRadius(14.dp).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (refreshing) R.drawable.ic_refreshing else R.drawable.ic_refresh),
            contentDescription = LocalContext.current.getString(R.string.refresh),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(18.dp),
        )
    }
}

/** Shown when a widget has nothing to display yet. Tapping it opens the app. */
@Composable
fun EmptyMessage(text: String, color: ColorProvider = GlanceTheme.colors.onSurfaceVariant) {
    Text(
        text,
        style = TextStyle(color = color, fontSize = 13.sp),
        modifier = GlanceModifier.padding(top = 8.dp),
    )
}

/** Opens the app, on a module's screen if [moduleId] is given. */
fun openAppIntent(context: Context, moduleId: String? = null): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .apply { if (moduleId != null) putExtra(MainActivity.EXTRA_MODULE, moduleId) }

/** "14:05" today, otherwise a short date and time. */
fun formatTime(context: Context, millis: Long): String =
    if (DateUtils.isToday(millis)) DateFormat.getTimeFormat(context).format(Date(millis))
    else DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)

/** The status line used by widgets and module screens for data that is fetched online. */
fun refreshStatus(context: Context, lastRefresh: Long, failed: Boolean, refreshing: Boolean): String = when {
    refreshing -> context.getString(R.string.updating)
    lastRefresh == 0L -> context.getString(R.string.not_updated_yet)
    failed -> context.getString(R.string.offline_since, formatTime(context, lastRefresh))
    else -> formatTime(context, lastRefresh)
}
