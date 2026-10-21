package dev.hydranet.worldtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun WorldDayNightCard(selected: Instant, homeZone: ZoneId) {
    val colors = MaterialTheme.colorScheme
    val isLight = colors.surface.luminance() > 0.5f
    val nightShade = if (isLight) colors.inverseSurface else colors.surfaceDim
    val dayOcean = if (isLight) lerp(colors.secondaryContainer, nightShade, 0.22f) else colors.secondaryContainer
    val dayLand = lerp(colors.primary, nightShade, 0.18f)
    val nightOverlay = nightShade.copy(alpha = 0.32f)
    val sun = remember(selected) { SolarPosition.at(selected) }
    val moon = remember(selected) { LunarPosition.at(selected) }
    val resources = LocalResources.current
    val cityLocations by produceState(emptyMap<String, MapLocation>(), resources) {
        value = withContext(Dispatchers.IO) {
            readCityLocations(resources.openRawResource(R.raw.time_zone_locations).reader())
        }
    }
    val homeLocation = cityLocations[homeZone.id]
    val sunIcon = rememberVectorPainter(Icons.Outlined.WbSunny)
    val moonIcon = rememberVectorPainter(Icons.Filled.NightlightRound)
    val nightPaths = remember(sun) { listOf(0.0, -6.0, -12.0).map { nightPath(sun, it) } }
    val land = painterResource(R.drawable.world_map_land)
    val zoneRegions = ImageVector.vectorResource(R.drawable.world_map_zone_regions)
    val homeOffsetMinutes = homeZone.rules.getStandardOffset(selected).totalSeconds / 60
    val homeRegion = remember(zoneRegions, homeOffsetMinutes) {
        Path().apply {
            fillType = PathFillType.EvenOdd
            for (index in 0 until zoneRegions.root.size) {
                val region = zoneRegions.root[index] as VectorGroup
                if (region.name == "utc_$homeOffsetMinutes") {
                    for (polygonIndex in 0 until region.size) {
                        val polygon = region[polygonIndex] as VectorPath
                        addPath(PathParser().addPathNodes(polygon.pathData).toPath())
                    }
                }
            }
        }
    }
    val timeZones = painterResource(R.drawable.world_map_time_zones)
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm 'UTC'").withZone(ZoneOffset.UTC) }
    val accessibleFormat = remember {
        DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC)
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "DAY & NIGHT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    timeFormat.format(selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(dayOcean)
                    .semantics {
                        contentDescription = "World daylight map for ${accessibleFormat.format(selected)}. " +
                            "Dark shading shows night, lighter shading shows twilight. " +
                            "Lines show approximate time zone boundaries. " +
                            "The highlighted band matches the standard UTC offset of your home zone, " +
                            WorldTimeData.cityName(homeZone) + ". " +
                            (if (homeLocation != null) "The city marker shows its reference location. " else "") +
                            "Sun and Moon icons mark where each is overhead at the selected time."
                    },
            ) {
                with(land) { draw(size, colorFilter = ColorFilter.tint(dayLand)) }
                clipRect {
                    scale(size.width / 360f, size.height / 180f, pivot = Offset.Zero) {
                        for (path in nightPaths) {
                            for (wrap in -1..1) {
                                translate(left = wrap * 360f) {
                                    drawPath(path, nightOverlay)
                                }
                            }
                        }
                        drawPath(
                            homeRegion,
                            colors.tertiary.copy(alpha = if (isLight) 0.22f else 0.18f),
                        )
                    }
                }
                with(timeZones) { draw(size, colorFilter = ColorFilter.tint(colors.outline)) }
                val sunCenter = drawCelestialMarker(
                    anchor = mapPoint(MapLocation(sun.latitude, sun.longitude)),
                    icon = sunIcon,
                    foreground = colors.tertiary,
                    background = colors.surfaceContainerHighest,
                )
                val moonAnchor = mapPoint(moon)
                val moonOffset = if ((moonAnchor - sunCenter).getDistance() < 24.dp.toPx()) {
                    if (moonAnchor.y + 36.dp.toPx() < size.height) 24.dp.toPx() else -24.dp.toPx()
                } else 0f
                drawCelestialMarker(
                    anchor = moonAnchor,
                    icon = moonIcon,
                    foreground = colors.primary,
                    background = colors.surfaceContainerHighest,
                    offsetY = moonOffset,
                )
                if (homeLocation != null) {
                    val cityCenter = mapPoint(homeLocation)
                    drawCircle(colors.surfaceContainerHighest, radius = 4.5.dp.toPx(), center = cityCenter)
                    drawCircle(colors.primary, radius = 2.5.dp.toPx(), center = cityCenter)
                }
            }
        }
    }
}

private fun nightPath(sun: SolarPosition, altitude: Double): Path = Path().apply {
    val center = sun.longitude + 360.0
    // Sample both sides of the night region, including polar day/night, in map coordinates.
    for (step in 0..360) {
        val y = step / 2.0
        val x = center - sun.nightHalfWidth(90.0 - y, altitude)
        if (step == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
    }
    for (step in 360 downTo 0) {
        val y = step / 2.0
        lineTo((center + sun.nightHalfWidth(90.0 - y, altitude)).toFloat(), y.toFloat())
    }
    close()
}

@Preview(showBackground = true)
@Composable
private fun WorldDayNightCardPreview() {
    WorldTimeTheme {
        WorldDayNightCard(Instant.parse("2026-06-21T12:00:00Z"), ZoneId.of("Europe/Zurich"))
    }
}
