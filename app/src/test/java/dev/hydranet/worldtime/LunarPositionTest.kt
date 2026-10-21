package dev.hydranet.worldtime

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarPositionTest {
    @Test
    fun matchesPublishedAlmanacPosition() {
        // 1990-04-19 almanac RA/declination and Greenwich sidereal time:
        // https://www.stjarnhimlen.se/comp/tutorial.html
        val moon = LunarPosition.at(Instant.parse("1990-04-19T00:00:00Z"))
        assertEquals(-19.0741, moon.latitude, 0.1)
        assertEquals(309.4881 - 206.8388, moon.longitude, 0.1)
    }

    @Test
    fun earthRotationMovesMoonWestAcrossMidnight() {
        val before = LunarPosition.at(Instant.parse("2028-02-29T23:30:00Z"))
        val after = LunarPosition.at(Instant.parse("2028-03-01T00:30:00Z"))
        val westwardChange = (before.longitude - after.longitude + 360.0) % 360.0
        assertTrue(westwardChange in 14.0..15.0)
        assertEquals(before.latitude, after.latitude, 0.6)
    }

    @Test
    fun moonMovesIndependentlyOfSunDuringLunarMonth() {
        val start = Instant.parse("2026-01-01T12:00:00Z")
        val separations = (0L..29L).map { day ->
            val instant = start.plusSeconds(day * 86_400)
            val moon = LunarPosition.at(instant)
            val sun = SolarPosition.at(instant)
            assertTrue(moon.latitude in -29.0..29.0)
            assertTrue(moon.longitude >= -180.0 && moon.longitude < 180.0)
            (moon.longitude - sun.longitude + 360.0) % 360.0
        }
        assertTrue(separations.any { it in 0.0..15.0 || it in 345.0..360.0 })
        assertTrue(separations.any { it in 165.0..195.0 })
    }
}
