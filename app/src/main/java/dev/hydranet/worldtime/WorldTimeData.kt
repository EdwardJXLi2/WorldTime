package dev.hydranet.worldtime

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object WorldTimeData {
    private const val PREFERENCES = "world_time"
    private const val ZONES_KEY = "zones"
    private const val HOME_ZONE_KEY = "home_zone"

    val availableZones: List<ZoneId> by lazy {
        (ZoneId.getAvailableZoneIds() + "UTC")
            .distinct()
            .map(ZoneId::of)
            .sortedWith(compareBy({ cityName(it) }, { it.id }))
    }

    fun loadZones(context: Context): List<ZoneId> {
        val saved = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(ZONES_KEY, null)
        if (saved == null) {
            return listOf(
                ZoneId.systemDefault(),
                ZoneId.of("America/New_York"),
                ZoneId.of("Europe/London"),
                ZoneId.of("Asia/Tokyo"),
            ).distinct()
        }
        return saved.split('|').mapNotNull { id ->
            runCatching { ZoneId.of(id) }.getOrNull()
        }.distinct().ifEmpty { listOf(ZoneId.systemDefault()) }
    }

    fun saveZones(context: Context, zones: List<ZoneId>) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putString(ZONES_KEY, zones.joinToString("|") { it.id }).apply()
    }

    fun loadHomeZone(context: Context): ZoneId {
        val saved = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(HOME_ZONE_KEY, null)
        return saved?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
    }

    fun saveHomeZone(context: Context, zone: ZoneId) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putString(HOME_ZONE_KEY, zone.id).apply()
    }

    fun cityName(zone: ZoneId): String = zone.id.substringAfterLast('/').replace('_', ' ')

    fun regionName(zone: ZoneId): String = zone.id.substringBeforeLast('/', "Global")
        .replace("/", " · ").replace('_', ' ')

    fun offsetLabel(zone: ZoneId, instant: Instant): String {
        val seconds = zone.rules.getOffset(instant).totalSeconds
        if (seconds == 0) return "UTC"
        val sign = if (seconds >= 0) "+" else "−"
        val minutes = kotlin.math.abs(seconds) / 60
        return "UTC$sign${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
    }

    fun timelineStart(instant: Instant, homeZone: ZoneId): Instant =
        instant.atZone(homeZone).toLocalDate().atStartOfDay(homeZone)
            .toInstant().minus(Duration.ofHours(6))

    fun timelineHourIndex(start: Instant, selected: Instant): Int =
        Duration.between(start, selected).toHours().toInt()

    fun onDate(instant: Instant, homeZone: ZoneId, date: LocalDate): Instant =
        date.atTime(instant.atZone(homeZone).toLocalTime()).atZone(homeZone).toInstant()

    fun onTime(instant: Instant, homeZone: ZoneId, time: LocalTime): Instant =
        instant.atZone(homeZone).toLocalDate().atTime(time).atZone(homeZone).toInstant()

}
