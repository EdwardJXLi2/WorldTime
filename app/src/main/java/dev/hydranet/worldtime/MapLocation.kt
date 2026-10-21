package dev.hydranet.worldtime

import java.io.Reader

internal data class MapLocation(val latitude: Double, val longitude: Double)

internal fun readCityLocations(reader: Reader): Map<String, MapLocation> = reader.buffered().useLines { lines ->
    lines.filter { it.isNotBlank() && !it.startsWith('#') }.associate { line ->
        val (zone, latitude, longitude) = line.split(',')
        zone to MapLocation(latitude.toDouble(), longitude.toDouble())
    }
}
