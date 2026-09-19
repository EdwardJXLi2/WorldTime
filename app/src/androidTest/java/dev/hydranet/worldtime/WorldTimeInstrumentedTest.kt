package dev.hydranet.worldtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorldTimeInstrumentedTest {
    @Test
    fun sharedLinkRestoresSelectedInstantHomeAndCityOrder() {
        val instant = Instant.parse("2026-09-18T19:26:00Z")
        val zones = listOf(ZoneId.of("Europe/Zurich"), ZoneId.of("Asia/Kathmandu"))

        val restored = WorldTimeShare.parse(WorldTimeShare.link(instant, zones[1], zones))

        assertEquals(instant, restored?.instant)
        assertEquals(zones[1], restored?.homeZone)
        assertEquals(zones, restored?.zones)
    }

    @Test
    fun shareTextIsConciseAndAllAvailableZonesCanBeBrowsed() {
        val instant = Instant.parse("2026-09-18T19:26:00Z")
        val zones = listOf(ZoneId.of("Europe/Zurich"), ZoneId.of("Asia/Kathmandu"))
        val message = WorldTimeShare.message(instant, zones[0], zones, false)

        assertTrue(message.startsWith("Zurich:"))
        assertTrue(message.contains("Kathmandu:"))
        assertTrue(message.contains("Open view: worldtime://view?"))
        assertFalse(message.contains("World Time"))
        assertTrue(WorldTimeData.availableZones.size > 100)
        assertTrue(ZoneId.of("Etc/GMT") in WorldTimeData.availableZones)
    }
}
