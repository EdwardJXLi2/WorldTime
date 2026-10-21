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
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import java.time.Instant
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
            val homeLocation = readCityLocations(
                context.resources.openRawResource(R.raw.time_zone_locations).reader(),
            )[home.id]
            val daylight = homeLocation?.let { DaylightTimes.on(now.atZone(home).toLocalDate(), home, it) }
            val details = dayNightDetails(context, now, home, daylight)
            val sunriseDescription = context.getString(R.string.day_night_event_description,
                context.getString(R.string.day_night_sunrise), details.sunrise)
            val sunsetDescription = context.getString(R.string.day_night_event_description,
                context.getString(R.string.day_night_sunset), details.sunset)
            val layouts = sizes.associateWith { size ->
                val horizontalMapWidth = minOf(size.width - 152f, (size.height - 48f) * 2f)
                val verticalMapWidth = minOf(size.width - 32f, (size.height - 88f) * 2f)
                if (size.width >= 300f && size.height >= 96f && horizontalMapWidth > verticalMapWidth)
                    R.layout.world_day_night_widget_horizontal else R.layout.world_day_night_widget
            }
            val baseViews = layouts.values.distinct().associateWith { layout ->
                val views = RemoteViews(context.packageName, layout)
                views.setColorStateList(R.id.day_night_widget_root, "setBackgroundTintList",
                    ColorStateList.valueOf(colors.surfaceContainer.toArgb()))
                views.setTextColor(R.id.day_night_widget_title, colors.onSurfaceVariant.toArgb())
                views.setTextColor(R.id.day_night_widget_time, colors.onSurfaceVariant.toArgb())
                views.setTextViewText(R.id.day_night_widget_title,
                    "${WorldTimeData.cityName(home)} ${WorldTimeData.offsetLabel(home, now).removeSuffix(":00")}")
                views.setContentDescription(R.id.day_night_widget_title, details.timeZone)
                views.setTextViewText(R.id.day_night_widget_time, details.localTime)
                views.setContentDescription(R.id.day_night_widget_time,
                    "${details.localDate} ${details.localTime}, ${details.timeZone}")
                val horizontal = layout == R.layout.world_day_night_widget_horizontal
                if (!horizontal) {
                    listOf(R.id.day_night_widget_sunrise_label, R.id.day_night_widget_sunset_label).forEach {
                        views.setTextColor(it, colors.onSurfaceVariant.toArgb())
                    }
                }
                views.setTextColor(R.id.day_night_widget_sunrise, colors.primary.toArgb())
                views.setTextColor(R.id.day_night_widget_sunset, colors.primary.toArgb())
                views.setTextViewText(R.id.day_night_widget_sunrise, if (horizontal) sunriseDescription else details.sunrise)
                views.setTextViewText(R.id.day_night_widget_sunset,
                    if (horizontal) sunsetDescription else details.sunset)
                views.setContentDescription(R.id.day_night_widget_sunrise, sunriseDescription)
                views.setContentDescription(R.id.day_night_widget_sunset, sunsetDescription)
                views.setContentDescription(R.id.day_night_widget_map,
                    context.getString(R.string.day_night_widget_map_description,
                        DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm 'UTC'")
                            .withZone(ZoneOffset.UTC).format(now), WorldTimeData.cityName(home)))
                views.setOnClickPendingIntent(R.id.day_night_widget_root, PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ))
                views
            }
            manager.updateAppWidget(id, RemoteViews(sizes.associateWith { size ->
                val layout = layouts.getValue(size)
                val horizontal = layout == R.layout.world_day_night_widget_horizontal
                val sizedViews = RemoteViews(baseViews.getValue(layout))
                val showHeader = size.height >= 64f
                val showDetails = size.height >= if (horizontal) 96f else 112f
                val headerGap = if (showHeader && size.height >= 80f) 8f else 0f
                val mapHeight = size.height - 16f - (if (showHeader) 24f + headerGap else 0f) -
                    (if (showDetails && !horizontal) 40f else 0f)
                val mapWidth = minOf(size.width - 32f - (if (horizontal) 120f else 0f), mapHeight * 2f)
                    .coerceAtLeast(44f)
                val density = context.resources.displayMetrics.density.coerceAtMost(2f)
                val pixelWidth = (mapWidth * density).roundToInt().coerceIn(44, 1000)
                sizedViews.setViewVisibility(R.id.day_night_widget_header, if (showHeader) View.VISIBLE else View.GONE)
                sizedViews.setViewVisibility(R.id.day_night_widget_details, if (showDetails) View.VISIBLE else View.GONE)
                sizedViews.setViewLayoutMargin(R.id.day_night_widget_header,
                    RemoteViews.MARGIN_BOTTOM, headerGap, TypedValue.COMPLEX_UNIT_DIP)
                sizedViews.setViewLayoutWidth(R.id.day_night_widget_content,
                    mapWidth + (if (horizontal) 120f else 0f), TypedValue.COMPLEX_UNIT_DIP)
                sizedViews.setViewLayoutHeight(
                    if (horizontal) R.id.day_night_widget_body else R.id.day_night_widget_map,
                    mapWidth / 2f, TypedValue.COMPLEX_UNIT_DIP)
                sizedViews.setImageViewBitmap(R.id.day_night_widget_map,
                    renderDayNightMap(context, pixelWidth, pixelWidth / mapWidth, now, home, homeLocation, colors))
                sizedViews
            }))
        }
    }
}
