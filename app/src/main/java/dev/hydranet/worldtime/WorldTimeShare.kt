package dev.hydranet.worldtime

import android.net.Uri
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SharedWorldTime(
    val instant: Instant,
    val homeZone: ZoneId,
    val zones: List<ZoneId>,
)

object WorldTimeShare {
    fun link(instant: Instant, homeZone: ZoneId, zones: List<ZoneId>): Uri =
        Uri.Builder()
            .scheme("worldtime")
            .authority("view")
            .appendQueryParameter("at", instant.toEpochMilli().toString())
            .appendQueryParameter("home", homeZone.id)
            .apply {
                zones.forEach { appendQueryParameter("zone", it.id) }
            }
            .build()

    fun parse(uri: Uri?): SharedWorldTime? {
        if (uri?.scheme != "worldtime" || uri.host != "view") return null
        val millis = uri.getQueryParameter("at")?.toLongOrNull() ?: return null
        val instant = runCatching { Instant.ofEpochMilli(millis) }.getOrNull() ?: return null
        val zones = uri.getQueryParameters("zone").take(30)
            .mapNotNull { runCatching { ZoneId.of(it) }.getOrNull() }
            .distinct()
        if (zones.isEmpty()) return null
        val homeZone = uri.getQueryParameter("home")
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?.takeIf { it in zones } ?: zones.first()
        return SharedWorldTime(instant, homeZone, zones)
    }

    fun message(
        instant: Instant,
        homeZone: ZoneId,
        zones: List<ZoneId>,
        is24Hour: Boolean,
    ): String {
        val format = DateTimeFormatter.ofPattern(
            if (is24Hour) "EEE, MMM d yyyy · HH:mm z" else "EEE, MMM d yyyy · h:mm a z",
        )
        return buildString {
            zones.forEach { zone ->
                appendLine("${WorldTimeData.cityName(zone)}: ${format.format(instant.atZone(zone))}")
            }
            appendLine()
            append("Open view: ${link(instant, homeZone, zones)}")
        }
    }
}
