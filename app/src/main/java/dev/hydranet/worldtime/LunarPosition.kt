package dev.hydranet.worldtime

import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

internal object LunarPosition {
    private val epoch = Instant.parse("1999-12-31T00:00:00Z")

    // Geocentric lunar orbit with the principal perturbations, for a world overview.
    // https://www.stjarnhimlen.se/comp/ppcomp.html
    fun at(instant: Instant): MapLocation {
        val days = (instant.epochSecond - epoch.epochSecond) / 86_400.0
        val node = radians(125.1228 - 0.0529538083 * days)
        val inclination = radians(5.1454)
        val perigee = radians(318.0634 + 0.1643573223 * days)
        val anomaly = radians(115.3654 + 13.0649929509 * days)
        val eccentricity = 0.0549
        var eccentricAnomaly = anomaly
        repeat(5) {
            eccentricAnomaly -= (eccentricAnomaly - eccentricity * sin(eccentricAnomaly) - anomaly) /
                (1.0 - eccentricity * cos(eccentricAnomaly))
        }
        val orbitX = cos(eccentricAnomaly) - eccentricity
        val orbitY = sqrt(1.0 - eccentricity * eccentricity) * sin(eccentricAnomaly)
        val orbitAngle = atan2(orbitY, orbitX) + perigee
        val x = cos(node) * cos(orbitAngle) - sin(node) * sin(orbitAngle) * cos(inclination)
        val y = sin(node) * cos(orbitAngle) + cos(node) * sin(orbitAngle) * cos(inclination)
        val z = sin(orbitAngle) * sin(inclination)

        val sunAnomaly = radians(356.0470 + 0.9856002585 * days)
        val sunLongitude = sunAnomaly + radians(282.9404 + 4.70935E-5 * days)
        val moonLongitude = node + perigee + anomaly
        val elongation = moonLongitude - sunLongitude
        val latitudeArgument = moonLongitude - node
        val longitudeCorrection =
            -1.274 * sin(anomaly - 2 * elongation) + 0.658 * sin(2 * elongation) -
                0.186 * sin(sunAnomaly) - 0.059 * sin(2 * anomaly - 2 * elongation) -
                0.057 * sin(anomaly - 2 * elongation + sunAnomaly) +
                0.053 * sin(anomaly + 2 * elongation) + 0.046 * sin(2 * elongation - sunAnomaly) +
                0.041 * sin(anomaly - sunAnomaly) - 0.035 * sin(elongation) -
                0.031 * sin(anomaly + sunAnomaly) - 0.015 * sin(2 * latitudeArgument - 2 * elongation) +
                0.011 * sin(anomaly - 4 * elongation)
        val latitudeCorrection =
            -0.173 * sin(latitudeArgument - 2 * elongation) -
                0.055 * sin(anomaly - latitudeArgument - 2 * elongation) -
                0.046 * sin(anomaly + latitudeArgument - 2 * elongation) +
                0.033 * sin(latitudeArgument + 2 * elongation) +
                0.017 * sin(2 * anomaly + latitudeArgument)
        val longitude = atan2(y, x) + Math.toRadians(longitudeCorrection)
        val latitude = atan2(z, hypot(x, y)) + Math.toRadians(latitudeCorrection)

        val obliquity = radians(23.4393 - 3.563E-7 * days)
        val equatorialX = cos(longitude) * cos(latitude)
        val equatorialY = sin(longitude) * cos(latitude) * cos(obliquity) - sin(latitude) * sin(obliquity)
        val equatorialZ = sin(longitude) * cos(latitude) * sin(obliquity) + sin(latitude) * cos(obliquity)
        val rightAscension = Math.toDegrees(atan2(equatorialY, equatorialX))
        val siderealLongitude = Math.toDegrees(sunLongitude) + 180.0 + 360.0 * (days - floor(days))
        return MapLocation(
            latitude = Math.toDegrees(atan2(equatorialZ, hypot(equatorialX, equatorialY))),
            longitude = normalize(rightAscension - siderealLongitude + 180.0) - 180.0,
        )
    }

    private fun radians(degrees: Double): Double = Math.toRadians(normalize(degrees))

    private fun normalize(degrees: Double): Double = (degrees % 360.0 + 360.0) % 360.0
}
