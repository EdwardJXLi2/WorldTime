package dev.hydranet.worldtime

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal data class DayNightDetails(
    val timeZone: String,
    val localDate: String,
    val localTime: String,
    val sunrise: String,
    val sunset: String,
)

internal fun dayNightDetails(
    context: Context, instant: Instant, zone: ZoneId, daylight: DaylightTimes?,
): DayNightDetails {
    val locale = context.resources.configuration.locales[0]
    val timeFormat = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "Hm" else "hm"),
        locale,
    ).withZone(zone)
    val noEvents = daylight != null && daylight.sunrise == null && daylight.sunset == null
    val missing = when {
        daylight == null -> context.getString(R.string.day_night_no_location)
        noEvents && daylight.continuousDaylight -> context.getString(R.string.day_night_polar_day)
        noEvents -> context.getString(R.string.day_night_polar_night)
        else -> null
    }
    fun eventTime(event: Instant?, missingId: Int): String = when {
        event != null -> context.getString(R.string.day_night_approximate_time, timeFormat.format(event))
        missing != null -> missing
        else -> context.getString(missingId)
    }
    return DayNightDetails(
        timeZone = "${zone.id} (${WorldTimeData.offsetLabel(zone, instant)})",
        localDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale).format(instant.atZone(zone)),
        localTime = timeFormat.format(instant),
        sunrise = eventTime(daylight?.sunrise, R.string.day_night_no_sunrise),
        sunset = eventTime(daylight?.sunset, R.string.day_night_no_sunset),
    )
}
