package dev.hydranet.worldtime

import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

internal data class SolarPosition(val latitude: Double, val longitude: Double) {
    // Half the longitude span below this solar altitude, centered opposite the Sun.
    fun nightHalfWidth(latitude: Double, altitude: Double = 0.0): Double {
        val lat = Math.toRadians(latitude)
        val declination = Math.toRadians(this.latitude)
        val numerator = sin(Math.toRadians(altitude)) - sin(lat) * sin(declination)
        val denominator = cos(lat) * cos(declination)
        if (denominator < 1e-12) return if (numerator >= 0.0) 180.0 else 0.0
        return 180.0 - Math.toDegrees(acos((numerator / denominator).coerceIn(-1.0, 1.0)))
    }

    companion object {
        // NOAA's fractional-year approximation, sufficient for a world overview map.
        // https://gml.noaa.gov/grad/solcalc/solareqns.PDF
        fun at(instant: Instant): SolarPosition {
            val utc = instant.atOffset(ZoneOffset.UTC)
            val minutes = utc.hour * 60.0 + utc.minute + utc.second / 60.0
            val yearAngle = 2.0 * PI / utc.toLocalDate().lengthOfYear() *
                (utc.dayOfYear - 1 + (minutes / 60.0 - 12.0) / 24.0)
            val equationOfTime = 229.18 * (
                0.000075 + 0.001868 * cos(yearAngle) - 0.032077 * sin(yearAngle) -
                    0.014615 * cos(2 * yearAngle) - 0.040849 * sin(2 * yearAngle)
                )
            val declination = 0.006918 - 0.399912 * cos(yearAngle) +
                0.070257 * sin(yearAngle) - 0.006758 * cos(2 * yearAngle) +
                0.000907 * sin(2 * yearAngle) - 0.002697 * cos(3 * yearAngle) +
                0.00148 * sin(3 * yearAngle)
            val longitude = (720.0 - minutes - equationOfTime) / 4.0
            return SolarPosition(
                latitude = Math.toDegrees(declination),
                longitude = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0,
            )
        }
    }
}
