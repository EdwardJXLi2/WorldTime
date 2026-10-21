package dev.hydranet.worldtime

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarPositionTest {
    @Test
    fun equinoxSunIsNearEquatorAndGreenwichAtNoon() {
        val sun = SolarPosition.at(Instant.parse("2026-03-20T12:00:00Z"))
        assertEquals(0.0, sun.latitude, 1.0)
        assertEquals(0.0, sun.longitude, 3.0)
        assertEquals(90.0, sun.nightHalfWidth(0.0), 0.001)
    }

    @Test
    fun solsticesReversePolarDayAndNight() {
        val summer = SolarPosition.at(Instant.parse("2026-06-21T12:00:00Z"))
        val winter = SolarPosition.at(Instant.parse("2026-12-21T12:00:00Z"))
        assertEquals(23.44, summer.latitude, 0.5)
        assertEquals(-23.44, winter.latitude, 0.5)
        for (latitude in listOf(80.0, 90.0)) {
            assertEquals(0.0, summer.nightHalfWidth(latitude), 0.001)
            assertEquals(180.0, summer.nightHalfWidth(-latitude), 0.001)
            assertEquals(180.0, winter.nightHalfWidth(latitude), 0.001)
            assertEquals(0.0, winter.nightHalfWidth(-latitude), 0.001)
        }
    }

    @Test
    fun sunMovesWestByFifteenDegreesPerHour() {
        val morning = SolarPosition.at(Instant.parse("2026-06-21T06:00:00Z"))
        val noon = SolarPosition.at(Instant.parse("2026-06-21T12:00:00Z"))
        assertEquals(90.0, morning.longitude - noon.longitude, 0.1)
    }

    @Test
    fun twilightBandsRemainInsideNightRegion() {
        val sun = SolarPosition.at(Instant.parse("2026-02-28T23:59:00Z"))
        for (latitude in -90..90) {
            val night = sun.nightHalfWidth(latitude.toDouble())
            val civil = sun.nightHalfWidth(latitude.toDouble(), -6.0)
            val nautical = sun.nightHalfWidth(latitude.toDouble(), -12.0)
            assertTrue(night in 0.0..180.0)
            assertTrue(civil in 0.0..night)
            assertTrue(nautical in 0.0..civil)
        }
    }

    @Test
    fun longitudeWrapsContinuouslyAcrossMidnightAndLeapDay() {
        val before = SolarPosition.at(Instant.parse("2028-02-29T23:59:00Z"))
        val after = SolarPosition.at(Instant.parse("2028-03-01T00:01:00Z"))
        assertTrue(before.longitude in -180.0..180.0)
        assertTrue(after.longitude in -180.0..180.0)
        val westwardChange = (before.longitude - after.longitude + 360.0) % 360.0
        assertEquals(0.5, westwardChange, 0.01)
        assertEquals(before.latitude, after.latitude, 0.01)
    }
}
