package dev.hydranet.worldtime

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WorldTimeDataTest {
    @Test
    fun pickingDateKeepsLocalClockTimeAcrossDaylightSavingChange() {
        val newYork = ZoneId.of("America/New_York")
        val before = LocalDate.of(2026, 3, 7).atTime(12, 30).atZone(newYork).toInstant()

        val after = WorldTimeData.onDate(before, newYork, LocalDate.of(2026, 3, 8))

        assertEquals(12, after.atZone(newYork).hour)
        assertEquals(30, after.atZone(newYork).minute)
        assertEquals(23, Duration.between(before, after).toHours())
    }

    @Test
    fun repeatedHourHasSeparateTimelineCells() {
        val newYork = ZoneId.of("America/New_York")
        val firstOneAm = Instant.parse("2026-11-01T05:00:00Z")
        val secondOneAm = Instant.parse("2026-11-01T06:00:00Z")
        val start = WorldTimeData.timelineStart(firstOneAm, newYork)

        assertEquals(1, firstOneAm.atZone(newYork).hour)
        assertEquals(1, secondOneAm.atZone(newYork).hour)
        assertEquals(1, WorldTimeData.timelineHourIndex(start, secondOneAm) -
            WorldTimeData.timelineHourIndex(start, firstOneAm))
    }

    @Test
    fun offsetIncludesFractionalHour() {
        assertEquals("UTC+5:45", WorldTimeData.offsetLabel(
            ZoneId.of("Asia/Kathmandu"), Instant.parse("2026-09-18T12:00:00Z")))
    }

    @Test
    fun pickingTimeKeepsHomeDate() {
        val zurich = ZoneId.of("Europe/Zurich")
        val selected = Instant.parse("2026-09-18T19:26:00Z")
        val changed = WorldTimeData.onTime(selected, zurich, LocalTime.of(9, 15))

        assertEquals(LocalDate.of(2026, 9, 18), changed.atZone(zurich).toLocalDate())
        assertEquals(LocalTime.of(9, 15), changed.atZone(zurich).toLocalTime())
    }
}
