package dev.hydranet.worldtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CityLocationsTest {
    private val locations = readCityLocations(File("src/main/res/raw/time_zone_locations.csv").reader())

    @Test
    fun bundledCoordinatesPreserveCitiesAndResolveLegacyNames() {
        val newYork = locations.getValue("America/New_York")
        assertEquals(40.71417, newYork.latitude, 0.00001)
        assertEquals(-74.00639, newYork.longitude, 0.00001)
        assertEquals(newYork, locations["US/Eastern"])
        assertEquals(locations["Asia/Kathmandu"], locations["Asia/Katmandu"])
        assertNotEquals(locations["Europe/Zurich"], locations["Europe/Busingen"])
        assertTrue(locations.values.all { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 })
    }

    @Test
    fun fixedOffsetsDoNotInventCityLocations() {
        assertNull(locations["UTC"])
        assertNull(locations["Etc/GMT+5"])
    }
}
