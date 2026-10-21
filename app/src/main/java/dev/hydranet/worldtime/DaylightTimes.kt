package dev.hydranet.worldtime

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

internal data class DaylightTimes(
    val sunrise: Instant?,
    val sunset: Instant?,
    val continuousDaylight: Boolean,
) {
    companion object {
        fun on(date: LocalDate, zone: ZoneId, location: MapLocation): DaylightTimes {
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val latitude = Math.toRadians(location.latitude)
            // NOAA's apparent sunrise/sunset includes refraction and the solar disk's radius.
            // https://gml.noaa.gov/grad/solcalc/solareqns.PDF
            val horizon = sin(Math.toRadians(-0.833))
            fun aboveHorizon(instant: Instant): Boolean {
                val sun = SolarPosition.at(instant)
                val declination = Math.toRadians(sun.latitude)
                val hourAngle = Math.toRadians(location.longitude - sun.longitude)
                return sin(latitude) * sin(declination) +
                    cos(latitude) * cos(declination) * cos(hourAngle) >= horizon
            }

            var sunrise: Instant? = null
            var sunset: Instant? = null
            var previous = start
            var wasDaylight = aboveHorizon(start)
            val daylightAtStart = wasDaylight
            while (previous < end) {
                val next = minOf(previous.plusSeconds(60), end)
                val isDaylight = aboveHorizon(next)
                if (isDaylight != wasDaylight) {
                    var low = previous.epochSecond
                    var high = next.epochSecond
                    while (high - low > 1) {
                        val middle = (low + high) / 2
                        if (aboveHorizon(Instant.ofEpochSecond(middle)) == wasDaylight) low = middle
                        else high = middle
                    }
                    val crossing = Instant.ofEpochSecond(high)
                    if (crossing < end) {
                        if (isDaylight && sunrise == null) sunrise = crossing
                        if (!isDaylight && sunset == null) sunset = crossing
                    }
                }
                previous = next
                wasDaylight = isDaylight
            }
            return DaylightTimes(sunrise, sunset, sunrise == null && sunset == null && daylightAtStart)
        }
    }
}
