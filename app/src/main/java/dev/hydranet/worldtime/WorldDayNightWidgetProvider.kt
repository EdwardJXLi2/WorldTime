package dev.hydranet.worldtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class WorldDayNightWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
        scheduleRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle,
    ) {
        update(context, manager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> refreshAll(context)
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    companion object {
        private const val ACTION_REFRESH = "dev.hydranet.worldtime.REFRESH_DAY_NIGHT_WIDGET"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WorldDayNightWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { update(context, manager, it) }
            scheduleRefresh(context)
        }

        private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, WorldDayNightWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun scheduleRefresh(context: Context) {
            // Non-wakeup alarms allow Android to batch map updates while the device sleeps.
            val interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES
            val next = (System.currentTimeMillis() / interval + 1) * interval
            context.getSystemService(AlarmManager::class.java).setInexactRepeating(
                AlarmManager.RTC, next, interval, refreshIntent(context),
            )
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
                ?.filter { it.width > 0f && it.height > 0f }?.distinct()?.take(16)
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(
                    SizeF(
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).toFloat(),
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 96).toFloat(),
                    ),
                    SizeF(
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320).toFloat(),
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 96).toFloat(),
                    ),
                ).distinct()
            val zones = WorldTimeData.loadZones(context)
            val home = WorldTimeData.loadHomeZone(context).takeIf { it in zones } ?: zones.first()
            val now = Instant.now()
            val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            val colors = worldTimeColorScheme(context, dark)
            manager.updateAppWidget(id, RemoteViews(sizes.associateWith { size ->
                createViews(context, size, now, home, colors)
            }))
        }

        private fun createViews(
            context: Context, size: SizeF, now: Instant, home: ZoneId, colors: ColorScheme,
        ): RemoteViews {
            val showHeader = size.height >= 64f
            val mapHeight = size.height - 16f - if (showHeader) 24f else 0f
            val mapWidth = minOf(size.width - 16f, mapHeight * 2f).coerceAtLeast(44f)
            val density = context.resources.displayMetrics.density.coerceAtMost(2f)
            val pixelWidth = (mapWidth * density).roundToInt().coerceIn(44, 1000)
            val views = RemoteViews(context.packageName, R.layout.world_day_night_widget)
            views.setViewVisibility(R.id.day_night_widget_header, if (showHeader) View.VISIBLE else View.GONE)
            views.setColorStateList(R.id.day_night_widget_root, "setBackgroundTintList",
                ColorStateList.valueOf(colors.surfaceContainer.toArgb()))
            views.setTextColor(R.id.day_night_widget_title, colors.onSurfaceVariant.toArgb())
            views.setTextColor(R.id.day_night_widget_time, colors.onSurfaceVariant.toArgb())
            views.setTextViewText(R.id.day_night_widget_time,
                DateTimeFormatter.ofPattern("HH:mm 'UTC'").withZone(ZoneOffset.UTC).format(now))
            views.setImageViewBitmap(R.id.day_night_widget_map,
                renderDayNightMap(context, pixelWidth, pixelWidth / mapWidth, now, home, colors))
            views.setContentDescription(R.id.day_night_widget_map,
                context.getString(R.string.day_night_widget_map_description,
                    DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm 'UTC'")
                        .withZone(ZoneOffset.UTC).format(now), WorldTimeData.cityName(home)))
            views.setOnClickPendingIntent(R.id.day_night_widget_root, PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ))
            return views
        }
    }
}
