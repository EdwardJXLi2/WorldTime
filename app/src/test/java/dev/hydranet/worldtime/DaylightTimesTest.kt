package dev.hydranet.worldtime

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DaylightTimesTest {
    @Test
    fun equatorialEquinoxHasSunriseNearSixAndSunsetNearEighteen() {
        val date = LocalDate.of(2026, 3, 20)
        val times = DaylightTimes.on(date, ZoneOffset.UTC, MapLocation(0.0, 0.0))
        val sunrise = requireNotNull(times.sunrise).atOffset(ZoneOffset.UTC)
        val sunset = requireNotNull(times.sunset).atOffset(ZoneOffset.UTC)

        assertEquals(date, sunrise.toLocalDate())
        assertEquals(date, sunset.toLocalDate())
        assertEquals(6 * 60.0, sunrise.hour * 60.0 + sunrise.minute, 15.0)
        assertEquals(18 * 60.0, sunset.hour * 60.0 + sunset.minute, 15.0)
        assertFalse(times.continuousDaylight)
    }

    @Test
    fun zurichEventsUseSummerAndWinterLocalOffsets() {
        val zone = ZoneId.of("Europe/Zurich")
        val location = MapLocation(47.36667, 8.55)
        val summer = DaylightTimes.on(LocalDate.of(2026, 6, 21), zone, location)
        val winter = DaylightTimes.on(LocalDate.of(2026, 12, 21), zone, location)

        assertEquals(5 * 60.0 + 30, requireNotNull(summer.sunrise).atZone(zone).toLocalTime().toSecondOfDay() / 60.0, 10.0)
        assertEquals(21 * 60.0 + 25, requireNotNull(summer.sunset).atZone(zone).toLocalTime().toSecondOfDay() / 60.0, 10.0)
        assertEquals(8 * 60.0 + 10, requireNotNull(winter.sunrise).atZone(zone).toLocalTime().toSecondOfDay() / 60.0, 10.0)
        assertEquals(16 * 60.0 + 40, requireNotNull(winter.sunset).atZone(zone).toLocalTime().toSecondOfDay() / 60.0, 10.0)
    }

    @Test
    fun polarDayAndNightDoNotInventEvents() {
        val zone = ZoneId.of("Arctic/Longyearbyen")
        val location = MapLocation(78.2232, 15.6469)
        val summer = DaylightTimes.on(LocalDate.of(2026, 6, 21), zone, location)
        val winter = DaylightTimes.on(LocalDate.of(2026, 12, 21), zone, location)

        assertNull(summer.sunrise)
        assertNull(summer.sunset)
        assertTrue(summer.continuousDaylight)
        assertNull(winter.sunrise)
        assertNull(winter.sunset)
        assertFalse(winter.continuousDaylight)
    }

    @Test
    fun eventsStayWithinLocalDateAcrossDaylightSavingAndDateLine() {
        val cases = listOf(
            Triple(ZoneId.of("America/New_York"), MapLocation(40.71417, -74.00639), LocalDate.of(2026, 3, 8)),
            Triple(ZoneId.of("America/New_York"), MapLocation(40.71417, -74.00639), LocalDate.of(2026, 11, 1)),
            Triple(ZoneId.of("Pacific/Kiritimati"), MapLocation(1.8721, -157.4278), LocalDate.of(2026, 3, 20)),
            Triple(ZoneId.of("Pacific/Pago_Pago"), MapLocation(-14.2781, -170.7025), LocalDate.of(2026, 3, 20)),
        )
        for ((zone, location, date) in cases) {
            val times = DaylightTimes.on(date, zone, location)
            assertNotNull(times.sunrise)
            assertNotNull(times.sunset)
            val sunrise = requireNotNull(times.sunrise).atZone(zone)
            val sunset = requireNotNull(times.sunset).atZone(zone)
            assertEquals(date, sunrise.toLocalDate())
            assertEquals(date, sunset.toLocalDate())
            assertTrue(sunrise.toLocalTime() in LocalTime.of(5, 0)..LocalTime.of(8, 0))
            assertTrue(sunset.toLocalTime() in LocalTime.of(16, 0)..LocalTime.of(20, 0))
            assertTrue(Duration.between(sunrise, sunset).toHours() in 9..14)
        }
    }
}
