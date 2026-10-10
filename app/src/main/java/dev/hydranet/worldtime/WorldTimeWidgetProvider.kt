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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.StyleSpan
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.roundToInt

/** Native TextClocks keep local times live independently of chart refreshes. */
class WorldTimeWidgetProvider : AppWidgetProvider() {
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
        private const val ACTION_REFRESH = "dev.hydranet.worldtime.REFRESH_WIDGET"
        private const val HEADER_HEIGHT = 48f
        private const val ROW_HEIGHT = 64f
        private const val LABEL_WIDTH = 120f

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WorldTimeWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { update(context, manager, it) }
            scheduleRefresh(context)
        }

        private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, WorldTimeWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun scheduleRefresh(context: Context) {
            // Android may batch these non-wakeup alarms. TextClocks tick in the launcher.
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
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300).toFloat().coerceAtLeast(230f),
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 250).toFloat().coerceAtLeast(180f),
                    ),
                    SizeF(
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 352).toFloat().coerceAtLeast(230f),
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250).toFloat().coerceAtLeast(180f),
                    ),
                ).distinct()
            val zones = WorldTimeData.loadZones(context)
            val home = WorldTimeData.loadHomeZone(context).takeIf { it in zones } ?: zones.first()
            val now = Instant.now()
            manager.updateAppWidget(id, RemoteViews(sizes.associateWith { size ->
                createViews(context, size, zones, home, now)
            }))
        }

        private fun createViews(
            context: Context, size: SizeF, zones: List<ZoneId>, home: ZoneId, now: Instant,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.world_time_widget)
            val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            val colors = worldTimeColorScheme(context, dark)
            views.setColorStateList(R.id.widget_root, "setBackgroundTintList",
                ColorStateList.valueOf(colors.surfaceContainer.toArgb()))
            listOf(R.id.widget_title, R.id.widget_label_header, R.id.widget_more).forEach { viewId ->
                views.setTextColor(viewId, colors.onSurfaceVariant.toArgb())
            }
            val bodyHeight = size.height - 16f - 32f - HEADER_HEIGHT
            val screenDensity = context.resources.displayMetrics.density
            val rowHeight = ((bodyHeight / zones.size).coerceIn(56f, ROW_HEIGHT) * screenDensity)
                .roundToInt() / screenDensity
            val countWithoutFooter = floor(bodyHeight / rowHeight).toInt().coerceAtLeast(1)
            val count = if (countWithoutFooter < zones.size) {
                floor((bodyHeight - 20f) / rowHeight).toInt().coerceAtLeast(1)
            } else countWithoutFooter
            val visible = zones.take(count)
            val homeDate = now.atZone(home).toLocalDate()
            views.removeAllViews(R.id.widget_labels)
            visible.forEach { zone ->
                val local = now.atZone(zone)
                val day = when (ChronoUnit.DAYS.between(homeDate, local.toLocalDate())) {
                    -1L -> context.getString(R.string.widget_yesterday)
                    0L -> context.getString(R.string.widget_today)
                    1L -> context.getString(R.string.widget_tomorrow)
                    else -> DateTimeFormatter.ofPattern("MMM d").format(local)
                }
                val city = WorldTimeData.cityName(zone)
                val row = RemoteViews(context.packageName, R.layout.world_time_widget_label)
                row.setViewLayoutHeight(R.id.widget_city_row, rowHeight, TypedValue.COMPLEX_UNIT_DIP)
                row.setTextViewText(R.id.widget_city, if (zone == home) "⌂ $city" else city)
                row.setString(R.id.widget_clock, "setTimeZone", zone.id)
                row.setTextColor(R.id.widget_city, colors.onSurface.toArgb())
                row.setTextColor(R.id.widget_clock, colors.primary.toArgb())
                row.setTextColor(R.id.widget_offset, colors.onSurfaceVariant.toArgb())
                row.setTextViewText(R.id.widget_offset,
                    "${WorldTimeData.offsetLabel(zone, now).removeSuffix(":00")} · $day")
                row.setContentDescription(R.id.widget_city_row, "$city, ${zone.id}, $day")
                views.addView(R.id.widget_labels, row)
            }
            val width = (size.width - 16f - LABEL_WIDTH).coerceAtLeast(80f)
            val height = HEADER_HEIGHT + rowHeight * visible.size
            views.setViewLayoutHeight(R.id.widget_timeline, height, TypedValue.COMPLEX_UNIT_DIP)
            populateTimeline(context, views, width, rowHeight, visible, home, now, colors)
            views.setContentDescription(R.id.widget_timeline,
                "Hourly comparison around now in ${WorldTimeData.cityName(home)}. " +
                    visible.joinToString { WorldTimeData.cityName(it) })
            val hidden = zones.size - visible.size
            views.setViewVisibility(R.id.widget_more, if (hidden > 0) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_more, context.getString(R.string.widget_more, hidden))
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ))
            return views
        }

        private fun populateTimeline(
            context: Context, views: RemoteViews, width: Float, rowHeight: Float,
            zones: List<ZoneId>, home: ZoneId, now: Instant,
            colors: ColorScheme,
        ) {
            val columns = floor(width / 40f).toInt().coerceIn(3, 9)
                .let { if (it % 2 == 0) it - 1 else it }
            val center = columns / 2
            val currentHour = now.atZone(home).withMinute(0).withSecond(0).withNano(0).toInstant()
            val slots = List(columns) { currentHour.plus(it.toLong() - center, ChronoUnit.HOURS) }
            val is24Hour = DateFormat.is24HourFormat(context)
            fun emphasized(value: String, selected: Boolean): CharSequence = if (selected) {
                SpannableString(value).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else value
            views.removeAllViews(R.id.widget_timeline_columns)
            slots.forEachIndexed { index, instant ->
                val selected = index == center
                val column = RemoteViews(context.packageName, R.layout.world_time_widget_column)
                val header = RemoteViews(context.packageName, R.layout.world_time_widget_hour_header)
                val headerDate = instant.atZone(home).toLocalDate()
                val headerColor = if (headerDate.toEpochDay() % 2L == 0L) colors.surfaceContainer
                    else colors.surfaceContainerHigh
                header.setInt(R.id.widget_hour_header, "setBackgroundColor", headerColor.toArgb())
                header.setTextColor(R.id.widget_hour_hint, colors.primary.toArgb())
                header.setTextColor(R.id.widget_header_hour, colors.onSurface.toArgb())
                header.setTextViewText(R.id.widget_hour_hint,
                    if (selected) context.getString(R.string.widget_now) else "")
                header.setTextViewText(R.id.widget_header_hour,
                    emphasized(instant.atZone(home).hour.toString().padStart(2, '0'), selected))
                column.addView(R.id.widget_column, header)
                zones.forEach { zone ->
                    val local = instant.atZone(zone)
                    val newDay = index > 0 &&
                        slots[index - 1].atZone(zone).toLocalDate() != local.toLocalDate()
                    val cell = RemoteViews(context.packageName, R.layout.world_time_widget_hour)
                    cell.setViewLayoutHeight(R.id.widget_hour_row, rowHeight, TypedValue.COMPLEX_UNIT_DIP)
                    val hour = if (is24Hour) local.hour.toString().padStart(2, '0')
                        else ((local.hour + 11) % 12 + 1).toString()
                    val value = if (local.minute == 0) hour
                        else "$hour:${local.minute.toString().padStart(2, '0')}"
                    val period = if (newDay) DateTimeFormatter.ofPattern("MMM d").format(local)
                        else if (!is24Hour) { if (local.hour < 12) "am" else "pm" } else ""
                    cell.setTextViewText(R.id.widget_hour_value, emphasized(value, selected))
                    cell.setTextViewText(R.id.widget_hour_period, period)
                    cell.setViewVisibility(R.id.widget_hour_period, if (period.isEmpty()) View.GONE else View.VISIBLE)
                    cell.setTextViewTextSize(R.id.widget_hour_value, TypedValue.COMPLEX_UNIT_SP,
                        if (local.minute == 0) 16f else 12f)
                    cell.setInt(R.id.widget_hour_band, "setBackgroundResource",
                        bandBackground(local.hour, index, columns))
                    val (background, foreground) = when (local.hour) {
                        in 6..8 -> colors.secondaryContainer.copy(alpha = 0.62f) to colors.onSecondaryContainer
                        in 9..17 -> colors.surfaceVariant.copy(alpha = 0.28f) to colors.onSurface
                        in 18..21 -> colors.primaryContainer.copy(alpha = 0.58f) to colors.onPrimaryContainer
                        else -> colors.primaryContainer.copy(alpha = 0.92f) to colors.onPrimaryContainer
                    }
                    cell.setColorStateList(R.id.widget_hour_band, "setBackgroundTintList",
                        ColorStateList.valueOf(background.toArgb()))
                    cell.setTextColor(R.id.widget_hour_value, foreground.toArgb())
                    cell.setTextColor(R.id.widget_hour_period, foreground.copy(alpha = 0.78f).toArgb())
                    column.addView(R.id.widget_column, cell)
                }
                views.addView(R.id.widget_timeline_columns, column)
            }
            // Only the transparent outline is rasterized; every glyph is native launcher text.
            val height = HEADER_HEIGHT + rowHeight * zones.size
            val density = context.resources.displayMetrics.density.coerceAtMost(1.5f)
            val marker = createBitmap((width * density).roundToInt().coerceAtLeast(1),
                (height * density).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(marker)
            canvas.scale(density, density)
            val slotWidth = width / columns
            val bounds = RectF(center * slotWidth + 0.75f, 0.75f,
                (center + 1) * slotWidth - 0.75f, height - 0.75f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.primary.copy(alpha = 0.10f).toArgb()
            }
            canvas.drawRoundRect(bounds, 8f, 8f, paint)
            paint.color = colors.primary.toArgb()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(bounds, 8f, 8f, paint)
            views.setImageViewBitmap(R.id.widget_marker, marker)
        }

        private fun bandBackground(hour: Int, index: Int, columns: Int): Int {
            val backgrounds = when (hour) {
                in 0..5 -> intArrayOf(R.drawable.widget_band_night_left,
                    R.drawable.widget_band_night_middle, R.drawable.widget_band_night_right)
                in 6..8 -> intArrayOf(R.drawable.widget_band_morning_left,
                    R.drawable.widget_band_morning_middle, R.drawable.widget_band_morning_right)
                in 9..17 -> intArrayOf(R.drawable.widget_band_day_left,
                    R.drawable.widget_band_day_middle, R.drawable.widget_band_day_right)
                in 18..21 -> intArrayOf(R.drawable.widget_band_evening_left,
                    R.drawable.widget_band_evening_middle, R.drawable.widget_band_evening_right)
                else -> intArrayOf(R.drawable.widget_band_late_left,
                    R.drawable.widget_band_late_middle, R.drawable.widget_band_late_right)
            }
            return backgrounds[when (index) { 0 -> 0; columns - 1 -> 2; else -> 1 }]
        }
    }
}
