package dev.hydranet.worldtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DayOcean = Color(0xFFCEE3ED)
private val DayLand = Color(0xFF8DAE9B)
private val NightShade = Color(0xFF101E38)
private val SunColor = Color(0xFFFFD580)
private val TwilightLand = NightShade.copy(alpha = 0.32f).compositeOver(DayLand)
private val NightLand = NightShade.copy(alpha = 0.32f)
    .compositeOver(NightShade.copy(alpha = 0.32f).compositeOver(TwilightLand))

@Composable
internal fun WorldDayNightCard(selected: Instant) {
    val sun = remember(selected) { SolarPosition.at(selected) }
    val nightPaths = remember(sun) { listOf(0.0, -6.0, -12.0).map { nightPath(sun, it) } }
    val land = painterResource(R.drawable.world_map_land)
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
                    .background(DayOcean)
                    .semantics {
                        contentDescription = "World daylight map for ${accessibleFormat.format(selected)}. " +
                            "Dark shading shows night, lighter shading shows twilight. " +
                            "Lines show approximate time zone boundaries."
                    },
            ) {
                with(land) { draw(size, colorFilter = ColorFilter.tint(DayLand)) }
                clipRect {
                    scale(size.width / 360f, size.height / 180f, pivot = Offset.Zero) {
                        for (path in nightPaths) {
                            for (wrap in -1..1) {
                                translate(left = wrap * 360f) {
                                    drawPath(path, NightShade.copy(alpha = 0.32f))
                                }
                            }
                        }
                    }
                }
                with(timeZones) { draw(size, colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.65f))) }
                val sunCenter = Offset(
                    ((sun.longitude + 180) / 360 * size.width).toFloat(),
                    ((90 - sun.latitude) / 180 * size.height).toFloat(),
                )
                drawCircle(SunColor.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = sunCenter)
                drawCircle(NightShade.copy(alpha = 0.5f), radius = 4.dp.toPx(), center = sunCenter)
                drawCircle(SunColor, radius = 3.dp.toPx(), center = sunCenter)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MapLegend("Day", DayLand)
                MapLegend("Twilight", TwilightLand)
                MapLegend("Night", NightLand)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Approximate time zone boundaries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MapLegend(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        WorldDayNightCard(Instant.parse("2026-06-21T12:00:00Z"))
    }
}
